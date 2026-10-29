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
import com.decompress.app.data.ArchiveCompressionLevel
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.CompressionEngine
import com.decompress.app.data.CompressionFormat
import com.decompress.app.data.CompressionOptions
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CompressionTask(
    val sources: List<BrowserEntry>,
    val sourceTreeUri: Uri,
    val targetParentUri: Uri,
    val targetTreeUri: Uri,
    val options: CompressionOptions
) {
    val displayName: String
        get() = options.archiveName
}

object CompressionScheduler {
    const val ACTION_COMPRESSION_FINISHED =
        "com.decompress.app.action.COMPRESSION_FINISHED"
    const val EXTRA_COMPRESSION_SUCCESS = "compression_success"
    const val EXTRA_COMPRESSION_MESSAGE = "compression_message"

    const val QUEUE_NAME = "zippest-compression-queue"
    const val TAG_ALL_TASKS = "zippest-compression-task"
    const val TAG_NAME_PREFIX = "zippest-task-name:"

    private const val KEY_SOURCES = "sources"
    private const val KEY_SOURCE_TREE_URI = "source_tree_uri"
    private const val KEY_TARGET_PARENT_URI = "target_parent_uri"
    private const val KEY_TARGET_TREE_URI = "target_tree_uri"
    private const val KEY_FORMAT = "format"
    private const val KEY_ARCHIVE_NAME = "archive_name"
    private const val KEY_LEVEL = "level"
    private const val KEY_PASSWORD = "password"
    private const val KEY_SPLIT_SIZE = "split_size"

    fun enqueue(context: Context, task: CompressionTask): UUID {
        val input = workDataOf(
            KEY_SOURCES to encodeSources(task.sources),
            KEY_SOURCE_TREE_URI to task.sourceTreeUri.toString(),
            KEY_TARGET_PARENT_URI to task.targetParentUri.toString(),
            KEY_TARGET_TREE_URI to task.targetTreeUri.toString(),
            KEY_FORMAT to task.options.format.name,
            KEY_ARCHIVE_NAME to task.options.archiveName,
            KEY_LEVEL to task.options.level.name,
            KEY_PASSWORD to CompressionSecretBox.encrypt(task.options.password),
            KEY_SPLIT_SIZE to (task.options.splitSizeBytes ?: -1L),
            ExtractionScheduler.KEY_DISPLAY_NAME to task.displayName
        )
        val request = OneTimeWorkRequestBuilder<CompressionWorker>()
            .setInputData(input)
            .addTag(TAG_ALL_TASKS)
            .addTag(TAG_NAME_PREFIX + task.displayName.take(120))
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(QUEUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        return request.id
    }

    internal fun readTask(data: Data): CompressionTask {
        val sourceTreeUri = Uri.parse(
            data.getString(KEY_SOURCE_TREE_URI) ?: error("source tree missing")
        )
        val targetParentUri = Uri.parse(
            data.getString(KEY_TARGET_PARENT_URI) ?: error("target parent missing")
        )
        val targetTreeUri = Uri.parse(
            data.getString(KEY_TARGET_TREE_URI) ?: error("target tree missing")
        )
        val split = data.getLong(KEY_SPLIT_SIZE, -1L).takeIf { it > 0 }
        return CompressionTask(
            sources = decodeSources(data.getString(KEY_SOURCES).orEmpty()),
            sourceTreeUri = sourceTreeUri,
            targetParentUri = targetParentUri,
            targetTreeUri = targetTreeUri,
            options = CompressionOptions(
                format = CompressionFormat.valueOf(
                    data.getString(KEY_FORMAT) ?: CompressionFormat.ZIP.name
                ),
                archiveName = data.getString(KEY_ARCHIVE_NAME) ?: "压缩包.zip",
                level = ArchiveCompressionLevel.valueOf(
                    data.getString(KEY_LEVEL) ?: ArchiveCompressionLevel.NORMAL.name
                ),
                password = CompressionSecretBox.decrypt(
                    data.getString(KEY_PASSWORD).orEmpty()
                ),
                splitSizeBytes = split
            )
        )
    }

    private fun encodeSources(sources: List<BrowserEntry>): String {
        val array = JSONArray()
        sources.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("uri", entry.uri.toString())
                    put("name", entry.name)
                    put("directory", entry.isDirectory)
                    put("size", entry.size)
                    put("modified", entry.lastModified)
                    put("mime", entry.mimeType)
                }
            )
        }
        return array.toString()
    }

    private fun decodeSources(payload: String): List<BrowserEntry> {
        val array = JSONArray(payload)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            BrowserEntry(
                uri = Uri.parse(item.getString("uri")),
                name = item.getString("name"),
                isDirectory = item.getBoolean("directory"),
                size = item.optLong("size", 0L),
                lastModified = item.optLong("modified", 0L),
                mimeType = item.optString("mime", "").takeIf { it.isNotEmpty() }
            )
        }
    }
}

class CompressionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val notificationManager =
        appContext.getSystemService(NotificationManager::class.java)
    private val notificationId = (id.hashCode() and 0x7fffffff).coerceAtLeast(1)

    override suspend fun doWork(): Result {
        val task = runCatching { CompressionScheduler.readTask(inputData) }
            .getOrElse { error ->
                finish(false, "压缩失败：${error.message ?: "任务数据损坏"}")
                return Result.failure(
                    workDataOf(
                        ExtractionScheduler.KEY_MESSAGE to
                            (error.message ?: "任务数据损坏")
                    )
                )
            }

        val wakeLock = ArchiveWorkWakeLock.acquire(applicationContext, id.toString())
        return try {
            setForeground(
                foregroundInfo(
                    text = "准备压缩…",
                    progress = 0,
                    displayName = task.displayName
                )
            )
            setProgressAsync(
                workDataOf(
                    ExtractionScheduler.KEY_DISPLAY_NAME to task.displayName,
                    ExtractionScheduler.KEY_PROGRESS to 0,
                    ExtractionScheduler.KEY_MESSAGE to "准备压缩…",
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
                            progressNotification(
                                task.displayName,
                                "已暂停，点击任务队列继续",
                                currentProgress
                            )
                        )
                        setProgressAsync(
                            workDataOf(
                                ExtractionScheduler.KEY_DISPLAY_NAME to task.displayName,
                                ExtractionScheduler.KEY_PROGRESS to currentProgress,
                                ExtractionScheduler.KEY_MESSAGE to "已暂停",
                                ExtractionScheduler.KEY_PAUSED to true
                            )
                        )
                    }
                )
                if (!pauseHandled) throw CancellationException("压缩已取消")
                if (wasPaused) {
                    notificationManager.notify(
                        notificationId,
                        progressNotification(
                            task.displayName,
                            "正在压缩…",
                            currentProgress
                        )
                    )
                    setProgressAsync(
                        workDataOf(
                            ExtractionScheduler.KEY_DISPLAY_NAME to task.displayName,
                            ExtractionScheduler.KEY_PROGRESS to currentProgress,
                            ExtractionScheduler.KEY_MESSAGE to "正在压缩…",
                            ExtractionScheduler.KEY_PAUSED to false
                        )
                    )
                }
            }
            val result = CompressionEngine(applicationContext).compress(
                selected = task.sources,
                sourceTreeUri = task.sourceTreeUri,
                targetParentUri = task.targetParentUri,
                targetTreeUri = task.targetTreeUri,
                options = task.options.copy(onCheckpoint = checkpoint)
            ) { processed, total ->
                val pauseHandled = ArchiveTaskControl.waitWhilePaused(
                    applicationContext,
                    id,
                    isCancelled = ::isStopped,
                    onPaused = {
                        notificationManager.notify(
                            notificationId,
                            progressNotification(
                                task.displayName,
                                "已暂停，点击任务队列继续",
                                if (total > 0) {
                                    (processed * 100 / total).coerceIn(0, 99)
                                } else {
                                    0
                                }
                            )
                        )
                        setProgressAsync(
                            workDataOf(
                                ExtractionScheduler.KEY_DISPLAY_NAME to task.displayName,
                                ExtractionScheduler.KEY_PROGRESS to
                                    if (total > 0) {
                                        (processed * 100 / total).coerceIn(0, 99)
                                    } else {
                                        0
                                    },
                                ExtractionScheduler.KEY_MESSAGE to "已暂停",
                                ExtractionScheduler.KEY_PAUSED to true
                            )
                        )
                    }
                )
                if (!pauseHandled) throw CancellationException("压缩已取消")
                val progress = if (total > 0) {
                    (processed * 100f / total).toInt().coerceIn(0, 99)
                } else {
                    0
                }
                currentProgress = progress
                val now = SystemClock.elapsedRealtime()
                if (now - lastUpdate >= 250 || progress >= 99) {
                    lastUpdate = now
                    val message = if (total > 0) {
                        "正在压缩 $processed/$total 项"
                    } else {
                        "正在压缩…"
                    }
                    notificationManager.notify(
                        notificationId,
                        progressNotification(task.displayName, message, progress)
                    )
                    setProgressAsync(
                        workDataOf(
                            ExtractionScheduler.KEY_DISPLAY_NAME to task.displayName,
                            ExtractionScheduler.KEY_PROGRESS to progress,
                            ExtractionScheduler.KEY_MESSAGE to message,
                            ExtractionScheduler.KEY_PAUSED to false
                        )
                    )
                }
            }
            val message = if (task.options.splitSizeBytes == null) {
                "已创建 ${result.displayName}"
            } else {
                "已创建 ${result.partUris.size} 个分卷，打开 ${result.displayName} 可解压"
            }
            scan(result.partUris)
            finish(true, message)
            Result.success(
                workDataOf(ExtractionScheduler.KEY_MESSAGE to message)
            )
        } catch (error: CancellationException) {
            finish(false, "压缩已取消")
            throw error
        } catch (error: Exception) {
            finish(false, error.message ?: "无法完成压缩")
            Result.failure(
                workDataOf(
                    ExtractionScheduler.KEY_MESSAGE to
                        (error.message ?: "无法完成压缩")
                )
            )
        } finally {
            ArchiveTaskControl.clear(applicationContext, id)
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun finish(successful: Boolean, message: String) {
        val notify = AppSettingsRepository.read(applicationContext).notifyOnComplete
        if (notify) {
            ensureChannel()
            notificationManager.notify(
                notificationId,
                NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(if (successful) "压缩完成" else "压缩失败")
                    .setContentText(message)
                    .setContentIntent(openAppIntent())
                    .setAutoCancel(true)
                    .setPriority(
                        if (successful) NotificationCompat.PRIORITY_DEFAULT
                        else NotificationCompat.PRIORITY_HIGH
                    )
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .build()
            )
        }
        applicationContext.sendBroadcast(
            Intent(CompressionScheduler.ACTION_COMPRESSION_FINISHED)
                .setPackage(applicationContext.packageName)
                .putExtra(CompressionScheduler.EXTRA_COMPRESSION_SUCCESS, successful)
                .putExtra(CompressionScheduler.EXTRA_COMPRESSION_MESSAGE, message)
        )
    }

    private fun scan(uris: List<Uri>) {
        uris.mapNotNull { it.path }
            .distinct()
            .chunked(100)
            .forEach { paths ->
                MediaScannerConnection.scanFile(
                    applicationContext,
                    paths.toTypedArray(),
                    null,
                    null
                )
            }
    }

    private fun foregroundInfo(
        text: String,
        progress: Int,
        displayName: String
    ): ForegroundInfo {
        ensureChannel()
        return ForegroundInfo(
            notificationId,
            progressNotification(displayName, text, progress),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    private fun progressNotification(
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

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Zippest 压缩任务",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "显示后台压缩与分卷任务进度"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "compression_tasks"
    }
}

private object CompressionSecretBox {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "zippest_compression_passwords"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipher.iv + encrypted)
    }

    fun decrypt(payload: String): String {
        if (payload.isBlank()) return ""
        val bytes = Base64.getDecoder().decode(payload)
        require(bytes.size > IV_BYTES) { "密码任务数据损坏" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_BITS, bytes.copyOfRange(0, IV_BYTES))
        )
        return String(
            cipher.doFinal(bytes.copyOfRange(IV_BYTES, bytes.size)),
            Charsets.UTF_8
        )
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        ).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }
}
