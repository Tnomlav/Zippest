package com.decompress.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.decompress.app.MainActivity
import com.decompress.app.R
import com.decompress.app.data.ConflictPolicy
import com.decompress.app.data.DestinationMode
import com.decompress.app.data.ExtractSettings
import com.decompress.app.data.FileNameEncoding
import com.decompress.app.data.MultiArchiveEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

class ExtractionService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runningJob: Job? = null
    private var notificationId = 1
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) return START_NOT_STICKY

        val archiveUris = intent.getStringArrayListExtra(EXTRA_ARCHIVE_URIS)
            ?.map { Uri.parse(it) }
            .orEmpty()
        val archiveNames = intent.getStringArrayListExtra(EXTRA_ARCHIVE_NAMES).orEmpty()
        val passwords = intent.getStringArrayListExtra(EXTRA_PASSWORDS).orEmpty()
        if (
            archiveUris.isEmpty() ||
            archiveUris.size != archiveNames.size ||
            archiveUris.size != passwords.size
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        val workId = intent.getStringExtra(EXTRA_WORK_ID) ?: UUID.randomUUID().toString()
        notificationId = (workId.hashCode() and 0x7fffffff).coerceAtLeast(1)

        startForegroundWithProgress("准备解压…")
        runningJob?.cancel()
        runningJob = serviceScope.launch {
            extractAll(intent, archiveUris, archiveNames)
        }
        return START_NOT_STICKY
    }

    private suspend fun extractAll(
        intent: Intent,
        archiveUris: List<Uri>,
        archiveNames: List<String>
    ) {
        try {
            val mode = DestinationMode.valueOf(
                intent.getStringExtra(EXTRA_MODE) ?: DestinationMode.NEW_FOLDER.name
            )
            val conflict = ConflictPolicy.valueOf(
                intent.getStringExtra(EXTRA_CONFLICT) ?: ConflictPolicy.RENAME.name
            )
            val encoding = FileNameEncoding.valueOf(
                intent.getStringExtra(EXTRA_ENCODING) ?: FileNameEncoding.AUTO.name
            )
            val parentUri = intent.getStringExtra(EXTRA_PARENT_URI)?.let(Uri::parse)
            val parentTreeUri = intent.getStringExtra(EXTRA_PARENT_TREE_URI)?.let(Uri::parse)
            val customTree = intent.getStringExtra(EXTRA_CUSTOM_TREE_URI)?.let(Uri::parse)
            val customRoot = intent.getStringExtra(EXTRA_CUSTOM_ROOT_URI)?.let(Uri::parse)
            val createFolder = intent.getBooleanExtra(EXTRA_CREATE_FOLDER, true)
            val passwords = intent.getStringArrayListExtra(EXTRA_PASSWORDS).orEmpty()
            if (passwords.size != archiveUris.size) {
                throw IllegalStateException("密码任务数据不完整")
            }
            val targetParent = if (mode == DestinationMode.CUSTOM) {
                customRoot ?: throw IllegalStateException("自定义位置不可用")
            } else {
                parentUri ?: throw IllegalStateException("压缩包所在文件夹不可写")
            }
            val targetTree = if (mode == DestinationMode.CUSTOM) {
                customTree ?: throw IllegalStateException("自定义位置不可用")
            } else {
                parentTreeUri ?: throw IllegalStateException("压缩包所在文件夹不可写")
            }

            val engine = MultiArchiveEngine(applicationContext)
            val total = archiveUris.size
            archiveUris.forEachIndexed { index, archiveUri ->
                updateProgress("正在解压 ${index + 1}/$total · ${archiveNames[index]}")
                engine.extract(
                    archiveUri = archiveUri,
                    archiveName = archiveNames[index],
                    settings = ExtractSettings(
                        mode = mode,
                        targetParentUri = targetParent,
                        targetTreeUri = targetTree,
                        createFolder = createFolder,
                        conflictPolicy = conflict,
                        encoding = encoding,
                        password = passwords[index]
                    ),
                    onProgress = { processed, entryTotal ->
                        val within = if (entryTotal > 0) {
                            processed.toFloat() / entryTotal.toFloat()
                        } else {
                            0f
                        }
                        val overall = ((index + within) / total * 100f).toInt()
                        updateProgress(
                            "正在解压 ${index + 1}/$total · ${archiveNames[index]}",
                            overall
                        )
                    }
                )
            }

            finishWithNotification(
                title = "解压完成",
                text = "已完成 $total 个压缩包",
                successful = true
            )
            sendBroadcast(
                Intent(ACTION_EXTRACTION_FINISHED)
                    .setPackage(packageName)
                    .putExtra(EXTRA_EXTRACTION_SUCCESS, true)
                    .putExtra(EXTRA_EXTRACTION_MESSAGE, "已完成 $total 个压缩包")
            )
        } catch (error: Exception) {
            val message = error.message ?: "无法完成解压"
            finishWithNotification(
                title = "解压失败",
                text = message,
                successful = false
            )
            sendBroadcast(
                Intent(ACTION_EXTRACTION_FINISHED)
                    .setPackage(packageName)
                    .putExtra(EXTRA_EXTRACTION_SUCCESS, false)
                    .putExtra(EXTRA_EXTRACTION_MESSAGE, message)
            )
        } finally {
            stopSelf()
        }
    }

    private fun startForegroundWithProgress(text: String) {
        ServiceCompat.startForeground(
            this,
            notificationId,
            buildProgressNotification(text, null),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    private fun updateProgress(text: String, progress: Int? = null) {
        notificationManager.notify(
            notificationId,
            buildProgressNotification(text, progress)
        )
    }

    private fun buildProgressNotification(text: String, progress: Int?): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("正在解压")
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (progress == null) {
            builder.setProgress(0, 0, true)
        } else {
            builder.setProgress(100, progress.coerceIn(0, 100), false)
        }
        return builder.build()
    }

    private fun finishWithNotification(title: String, text: String, successful: Boolean) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
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

        stopForeground(STOP_FOREGROUND_DETACH)
        notificationManager.notify(notificationId, notification)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "解压任务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示后台解压进度与完成状态"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.decompress.app.action.START_EXTRACTION"
        const val ACTION_EXTRACTION_FINISHED =
            "com.decompress.app.action.EXTRACTION_FINISHED"
        const val EXTRA_EXTRACTION_SUCCESS = "extraction_success"
        const val EXTRA_EXTRACTION_MESSAGE = "extraction_message"

        private const val CHANNEL_ID = "extraction_tasks"
        private const val EXTRA_ARCHIVE_URIS = "archive_uris"
        private const val EXTRA_ARCHIVE_NAMES = "archive_names"
        private const val EXTRA_PARENT_URI = "parent_uri"
        private const val EXTRA_PARENT_TREE_URI = "parent_tree_uri"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_CUSTOM_TREE_URI = "custom_tree_uri"
        private const val EXTRA_CUSTOM_ROOT_URI = "custom_root_uri"
        private const val EXTRA_CREATE_FOLDER = "create_folder"
        private const val EXTRA_CONFLICT = "conflict"
        private const val EXTRA_ENCODING = "encoding"
        private const val EXTRA_PASSWORDS = "passwords"
        private const val EXTRA_WORK_ID = "work_id"

        fun createIntent(
            context: Context,
            archiveUris: List<Uri>,
            archiveNames: List<String>,
            parentUri: Uri?,
            parentTreeUri: Uri?,
            mode: DestinationMode,
            customTreeUri: Uri?,
            customRootUri: Uri?,
            createFolder: Boolean,
            conflictPolicy: ConflictPolicy,
            encoding: FileNameEncoding,
            passwords: List<String>
        ): Intent {
            require(passwords.size == archiveUris.size) {
                "Every archive needs one password slot"
            }
            return Intent(context, ExtractionService::class.java).apply {
                action = ACTION_START
                putStringArrayListExtra(
                    EXTRA_ARCHIVE_URIS,
                    ArrayList(archiveUris.map { it.toString() })
                )
                putStringArrayListExtra(EXTRA_ARCHIVE_NAMES, ArrayList(archiveNames))
                putExtra(EXTRA_PARENT_URI, parentUri?.toString())
                putExtra(EXTRA_PARENT_TREE_URI, parentTreeUri?.toString())
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_CUSTOM_TREE_URI, customTreeUri?.toString())
                putExtra(EXTRA_CUSTOM_ROOT_URI, customRootUri?.toString())
                putExtra(EXTRA_CREATE_FOLDER, createFolder)
                putExtra(EXTRA_CONFLICT, conflictPolicy.name)
                putExtra(EXTRA_ENCODING, encoding.name)
                putStringArrayListExtra(EXTRA_PASSWORDS, ArrayList(passwords))
                putExtra(EXTRA_WORK_ID, UUID.randomUUID().toString())
            }
        }
    }
}
