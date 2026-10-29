package com.decompress.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.decompress.app.MainActivity
import com.decompress.app.R
import com.decompress.app.data.AppSettingsRepository
import com.decompress.app.data.ConflictPolicy
import com.decompress.app.data.DestinationMode
import com.decompress.app.data.ExtractSettings
import com.decompress.app.data.FileNameEncoding
import com.decompress.app.data.MultiArchiveEngine
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import java.security.KeyStore
import java.util.Base64
import java.util.Collections
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ExtractionTask(
    val archiveUris: List<Uri>,
    val archiveNames: List<String>,
    val parentUri: Uri?,
    val parentTreeUri: Uri?,
    val mode: DestinationMode,
    val customTreeUri: Uri?,
    val customRootUri: Uri?,
    val createFolder: Boolean,
    val conflictPolicy: ConflictPolicy,
    val encoding: FileNameEncoding,
    val passwords: List<String>
) {
    val displayName: String
        get() = if (archiveNames.size == 1) {
            archiveNames.first()
        } else {
            "${archiveNames.first()} 等 ${archiveNames.size} 个压缩包"
        }

    init {
        require(archiveUris.isNotEmpty()) { "No archives selected" }
        require(archiveUris.size == archiveNames.size) { "Archive metadata mismatch" }
        require(archiveUris.size == passwords.size) { "Password metadata mismatch" }
    }
}

object ExtractionScheduler {
    const val ACTION_EXTRACTION_FINISHED =
        "com.decompress.app.action.EXTRACTION_FINISHED"
    const val EXTRA_EXTRACTION_SUCCESS = "extraction_success"
    const val EXTRA_EXTRACTION_MESSAGE = "extraction_message"

    const val QUEUE_NAME = "zippest-extraction-queue"
    const val TAG_ALL_TASKS = "zippest-extraction-task"
    const val TAG_NAME_PREFIX = "zippest-task-name:"
    const val KEY_DISPLAY_NAME = "display_name"
    const val KEY_PROGRESS = "progress"
    const val KEY_MESSAGE = "message"
    const val KEY_PAUSED = "paused"

    private const val KEY_ARCHIVE_URIS = "archive_uris"
    private const val KEY_ARCHIVE_NAMES = "archive_names"
    private const val KEY_PARENT_URI = "parent_uri"
    private const val KEY_PARENT_TREE_URI = "parent_tree_uri"
    private const val KEY_MODE = "mode"
    private const val KEY_CUSTOM_TREE_URI = "custom_tree_uri"
    private const val KEY_CUSTOM_ROOT_URI = "custom_root_uri"
    private const val KEY_CREATE_FOLDER = "create_folder"
    private const val KEY_CONFLICT = "conflict"
    private const val KEY_ENCODING = "encoding"
    private const val KEY_PASSWORDS = "passwords"

    fun enqueue(context: Context, task: ExtractionTask): UUID {
        val input = workDataOf(
            KEY_ARCHIVE_URIS to task.archiveUris.map(Uri::toString).toTypedArray(),
            KEY_ARCHIVE_NAMES to task.archiveNames.toTypedArray(),
            KEY_PARENT_URI to task.parentUri?.toString(),
            KEY_PARENT_TREE_URI to task.parentTreeUri?.toString(),
            KEY_MODE to task.mode.name,
            KEY_CUSTOM_TREE_URI to task.customTreeUri?.toString(),
            KEY_CUSTOM_ROOT_URI to task.customRootUri?.toString(),
            KEY_CREATE_FOLDER to task.createFolder,
            KEY_CONFLICT to task.conflictPolicy.name,
            KEY_ENCODING to task.encoding.name,
            KEY_PASSWORDS to ExtractionSecretBox.encrypt(task.passwords),
            KEY_DISPLAY_NAME to task.displayName
        )
        val request = OneTimeWorkRequestBuilder<ExtractionWorker>()
            .setInputData(input)
            .addTag(TAG_ALL_TASKS)
            .addTag(TAG_NAME_PREFIX + task.displayName.take(120))
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(QUEUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        return request.id
    }

    internal fun readTask(data: Data): ExtractionTask {
        val archiveUris = data.getNullableStringArray(KEY_ARCHIVE_URIS)
            ?.filterNotNull()
            ?.map(Uri::parse)
            .orEmpty()
        val archiveNames = data.getNullableStringArray(KEY_ARCHIVE_NAMES)
            ?.filterNotNull()
            ?.toList()
            .orEmpty()
        val passwords = ExtractionSecretBox.decrypt(
            data.getString(KEY_PASSWORDS).orEmpty()
        )
        return ExtractionTask(
            archiveUris = archiveUris,
            archiveNames = archiveNames,
            parentUri = data.getString(KEY_PARENT_URI)?.let(Uri::parse),
            parentTreeUri = data.getString(KEY_PARENT_TREE_URI)?.let(Uri::parse),
            mode = DestinationMode.valueOf(
                data.getString(KEY_MODE) ?: DestinationMode.NEW_FOLDER.name
            ),
            customTreeUri = data.getString(KEY_CUSTOM_TREE_URI)?.let(Uri::parse),
            customRootUri = data.getString(KEY_CUSTOM_ROOT_URI)?.let(Uri::parse),
            createFolder = data.getBoolean(KEY_CREATE_FOLDER, true),
            conflictPolicy = ConflictPolicy.valueOf(
                data.getString(KEY_CONFLICT) ?: ConflictPolicy.RENAME.name
            ),
            encoding = FileNameEncoding.valueOf(
                data.getString(KEY_ENCODING) ?: FileNameEncoding.AUTO.name
            ),
            passwords = passwords
        )
    }
}

class ExtractionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val notificationManager =
        appContext.getSystemService(NotificationManager::class.java)
    private val notificationId = (id.hashCode() and 0x7fffffff).coerceAtLeast(1)

    override suspend fun doWork(): Result {
        val task = runCatching { ExtractionScheduler.readTask(inputData) }
            .getOrElse { error ->
                if (shouldNotifyFinished()) {
                    notifyFinished(
                        title = "解压失败",
                        text = error.message ?: "任务数据损坏",
                        successful = false
                    )
                }
                broadcastFinished(false, error.message ?: "任务数据损坏")
                return Result.failure(
                    workDataOf(
                        ExtractionScheduler.KEY_MESSAGE to
                            (error.message ?: "任务数据损坏")
                    )
                )
            }

        val resumed = ExtractionCheckpointStore.wasStarted(applicationContext, id)
        ExtractionCheckpointStore.markStarted(applicationContext, id)
        val effectiveTask = if (resumed) {
            task.copy(conflictPolicy = ConflictPolicy.OVERWRITE)
        } else {
            task
        }
        val wakeLock = ArchiveWorkWakeLock.acquire(applicationContext, id.toString())

        return try {
            setForeground(
                buildForegroundInfo(
                    text = "准备解压…",
                    progress = 0,
                    displayName = effectiveTask.displayName
                )
            )
            setProgressAsync(
                workDataOf(
                    ExtractionScheduler.KEY_DISPLAY_NAME to effectiveTask.displayName,
                    ExtractionScheduler.KEY_PROGRESS to 0,
                    ExtractionScheduler.KEY_MESSAGE to if (resumed) {
                        "恢复上次未完成的解压…"
                    } else {
                        "准备解压…"
                    },
                    ExtractionScheduler.KEY_PAUSED to false
                )
            )

            var lastUpdate = 0L
            var currentProgress = 0
            val checkpoint = {
                val wasPaused = ArchiveTaskControl.isPaused(applicationContext, id)
                val pauseHandled = ArchiveTaskControl.waitWhilePaused(
                    applicationContext,
                    id,
                    isCancelled = ::isStopped,
                    onPaused = {
                        notificationManager.notify(
                            notificationId,
                            buildProgressNotification(
                                effectiveTask.displayName,
                                "已暂停，点击任务队列继续",
                                currentProgress
                            )
                        )
                        setProgressAsync(
                            workDataOf(
                                ExtractionScheduler.KEY_DISPLAY_NAME to
                                    effectiveTask.displayName,
                                ExtractionScheduler.KEY_PROGRESS to currentProgress,
                                ExtractionScheduler.KEY_MESSAGE to "已暂停",
                                ExtractionScheduler.KEY_PAUSED to true
                            )
                        )
                    }
                )
                if (!pauseHandled) throw CancellationException("解压已取消")
                if (wasPaused) {
                    notificationManager.notify(
                        notificationId,
                        buildProgressNotification(
                            effectiveTask.displayName,
                            "正在解压…",
                            currentProgress
                        )
                    )
                    setProgressAsync(
                        workDataOf(
                            ExtractionScheduler.KEY_DISPLAY_NAME to
                                effectiveTask.displayName,
                            ExtractionScheduler.KEY_PROGRESS to currentProgress,
                            ExtractionScheduler.KEY_MESSAGE to "正在解压…",
                            ExtractionScheduler.KEY_PAUSED to false
                        )
                    )
                }
            }
            val resultMessage = extract(
                task = effectiveTask,
                onCheckpoint = checkpoint
            ) { progress, message ->
                currentProgress = progress
                val pauseHandled = ArchiveTaskControl.waitWhilePaused(
                    applicationContext,
                    id,
                    isCancelled = ::isStopped,
                    onPaused = {
                        notificationManager.notify(
                            notificationId,
                            buildProgressNotification(
                                effectiveTask.displayName,
                                "已暂停，点击任务队列继续",
                                progress
                            )
                        )
                        setProgressAsync(
                            workDataOf(
                                ExtractionScheduler.KEY_DISPLAY_NAME to
                                    effectiveTask.displayName,
                                ExtractionScheduler.KEY_PROGRESS to progress,
                                ExtractionScheduler.KEY_MESSAGE to "已暂停",
                                ExtractionScheduler.KEY_PAUSED to true
                            )
                        )
                    }
                )
                if (!pauseHandled) throw CancellationException("解压已取消")
                val now = SystemClock.elapsedRealtime()
                if (now - lastUpdate >= 250 || progress >= 100) {
                    lastUpdate = now
                    notificationManager.notify(
                        notificationId,
                        buildProgressNotification(
                            effectiveTask.displayName,
                            message,
                            progress
                        )
                    )
                    setProgressAsync(
                        workDataOf(
                            ExtractionScheduler.KEY_DISPLAY_NAME to
                                effectiveTask.displayName,
                            ExtractionScheduler.KEY_PROGRESS to progress,
                            ExtractionScheduler.KEY_MESSAGE to message,
                            ExtractionScheduler.KEY_PAUSED to false
                        )
                    )
                }
            }
            setProgressAsync(
                workDataOf(
                    ExtractionScheduler.KEY_DISPLAY_NAME to effectiveTask.displayName,
                    ExtractionScheduler.KEY_PROGRESS to 100,
                    ExtractionScheduler.KEY_MESSAGE to resultMessage,
                    ExtractionScheduler.KEY_PAUSED to false
                )
            )
            if (shouldNotifyFinished()) {
                notifyFinished("解压完成", resultMessage, successful = true)
            }
            broadcastFinished(true, resultMessage)
            ExtractionCheckpointStore.clear(applicationContext, id)
            Result.success(
                workDataOf(ExtractionScheduler.KEY_MESSAGE to resultMessage)
            )
        } catch (error: CancellationException) {
            if (shouldNotifyFinished()) {
                notifyFinished("解压已取消", effectiveTask.displayName, successful = false)
            }
            broadcastFinished(false, "解压已取消")
            ExtractionCheckpointStore.clear(applicationContext, id)
            throw error
        } catch (error: Exception) {
            val message = error.message ?: "无法完成解压"
            if (shouldNotifyFinished()) {
                notifyFinished("解压失败", message, successful = false)
            }
            broadcastFinished(false, message)
            ExtractionCheckpointStore.clear(applicationContext, id)
            Result.failure(
                workDataOf(ExtractionScheduler.KEY_MESSAGE to message)
            )
        } finally {
            ArchiveTaskControl.clear(applicationContext, id)
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private suspend fun extract(
        task: ExtractionTask,
        onCheckpoint: () -> Unit,
        onProgress: (Int, String) -> Unit
    ): String {
        val targetParent = if (task.mode == DestinationMode.CUSTOM) {
            task.customRootUri ?: throw IllegalStateException("自定义位置不可用")
        } else {
            task.parentUri ?: throw IllegalStateException("压缩包所在文件夹不可写")
        }
        val targetTree = if (task.mode == DestinationMode.CUSTOM) {
            task.customTreeUri ?: throw IllegalStateException("自定义位置不可用")
        } else {
            task.parentTreeUri ?: throw IllegalStateException("压缩包所在文件夹不可写")
        }

        val engine = MultiArchiveEngine(applicationContext)
        val total = task.archiveUris.size
        val createdFiles = Collections.synchronizedList(mutableListOf<Uri>())
        task.archiveUris.forEachIndexed { index, archiveUri ->
            if (!engine.supports(task.archiveNames[index])) {
                throw IllegalArgumentException("暂不支持 ${task.archiveNames[index]}")
            }
            onProgress(
                (index * 100f / total).toInt(),
                "正在解压 ${index + 1}/$total · ${task.archiveNames[index]}"
            )
            engine.extract(
                archiveUri = archiveUri,
                archiveName = task.archiveNames[index],
                settings = ExtractSettings(
                    mode = task.mode,
                    targetParentUri = targetParent,
                    targetTreeUri = targetTree,
                    createFolder = task.createFolder,
                    conflictPolicy = task.conflictPolicy,
                    encoding = task.encoding,
                    password = task.passwords[index],
                    onCheckpoint = onCheckpoint,
                    onFileCreated = { uri ->
                        if (uri.scheme == "file") {
                            createdFiles += uri
                        }
                    }
                ),
                onProgress = { processed, entryTotal ->
                    val within = if (entryTotal > 0) {
                        processed.toFloat() / entryTotal.toFloat()
                    } else {
                        0f
                    }
                    val overall = ((index + within) / total * 100f).toInt()
                    onProgress(
                        overall,
                        "正在解压 ${index + 1}/$total · ${task.archiveNames[index]}"
                    )
                },
                archiveTreeUri = task.parentTreeUri,
                archiveParentUri = task.parentUri
            )
        }
        scanCreatedFiles(createdFiles)
        return "已完成 $total 个压缩包"
    }

    private fun scanCreatedFiles(uris: List<Uri>) {
        val paths = synchronized(uris) {
            uris.mapNotNull { uri -> uri.path }
                .distinct()
        }
        paths.chunked(100).forEach { chunk ->
            MediaScannerConnection.scanFile(
                applicationContext,
                chunk.toTypedArray(),
                null,
                null
            )
        }
    }

    private fun buildForegroundInfo(
        text: String,
        progress: Int,
        displayName: String
    ): ForegroundInfo {
        ensureNotificationChannel()
        return ForegroundInfo(
            notificationId,
            buildProgressNotification(displayName, text, progress),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    private fun buildProgressNotification(
        displayName: String,
        text: String,
        progress: Int
    ): Notification =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zippest · ${displayName.take(48)}")
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(100, progress.coerceIn(0, 100), false)
            .build()

    private fun notifyFinished(title: String, text: String, successful: Boolean) {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(
                if (successful) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zippest 后台任务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示可恢复的后台解压进度与完成状态"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun broadcastFinished(successful: Boolean, message: String) {
        applicationContext.sendBroadcast(
            Intent(ExtractionScheduler.ACTION_EXTRACTION_FINISHED)
                .setPackage(applicationContext.packageName)
                .putExtra(ExtractionScheduler.EXTRA_EXTRACTION_SUCCESS, successful)
                .putExtra(ExtractionScheduler.EXTRA_EXTRACTION_MESSAGE, message)
        )
    }

    private fun shouldNotifyFinished(): Boolean =
        AppSettingsRepository.read(applicationContext).notifyOnComplete

    companion object {
        private const val CHANNEL_ID = "extraction_tasks"
    }
}

private object ExtractionSecretBox {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "zippest_extraction_passwords"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun encrypt(passwords: List<String>): String {
        val plaintext = JSONArray(passwords).toString()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    fun decrypt(payload: String): List<String> {
        if (payload.isBlank()) return emptyList()
        val bytes = Base64.getDecoder().decode(payload)
        require(bytes.size > IV_BYTES) { "密码任务数据损坏" }
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val encrypted = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        val json = JSONArray(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        return List(json.length()) { index -> json.getString(index) }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private const val IV_BYTES = 12
}

private object ExtractionCheckpointStore {
    private const val PREFERENCES = "extraction_checkpoints"

    fun wasStarted(context: Context, workId: UUID): Boolean =
        preferences(context).getBoolean(workId.toString(), false)

    fun markStarted(context: Context, workId: UUID) {
        preferences(context).edit().putBoolean(workId.toString(), true).commit()
    }

    fun clear(context: Context, workId: UUID) {
        preferences(context).edit().remove(workId.toString()).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
