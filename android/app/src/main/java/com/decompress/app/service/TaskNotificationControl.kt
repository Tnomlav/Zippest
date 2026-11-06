package com.decompress.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import java.util.UUID

/**
 * 通知栏上的「暂停 / 继续 / 取消」入口。
 *
 * 暂停只写一个 SharedPreferences 标志，worker 在下一个检查点（最长 1 MB 读取）停住；
 * 取消直接走 WorkManager 的 cancelWorkById，worker 的 finally 会清理未完成的临时文件。
 */
object TaskNotificationControl {
    const val ACTION_TOGGLE_PAUSE = "com.decompress.app.action.TOGGLE_PAUSE"
    const val ACTION_CANCEL = "com.decompress.app.action.CANCEL_TASK"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_PAUSED = "paused"

    fun pauseIntent(context: Context, taskId: UUID, paused: Boolean): PendingIntent {
        val intent = Intent(context, TaskControlReceiver::class.java).apply {
            action = ACTION_TOGGLE_PAUSE
            setPackage(context.packageName)
            putExtra(EXTRA_TASK_ID, taskId.toString())
            putExtra(EXTRA_PAUSED, paused)
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            taskId.hashCode() xor 0x2701,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancelIntent(context: Context, taskId: UUID): PendingIntent {
        val intent = Intent(context, TaskControlReceiver::class.java).apply {
            action = ACTION_CANCEL
            setPackage(context.packageName)
            putExtra(EXTRA_TASK_ID, taskId.toString())
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            taskId.hashCode() xor 0x2702,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class TaskControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskNotificationControl.EXTRA_TASK_ID)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: return
        when (intent.action) {
            TaskNotificationControl.ACTION_TOGGLE_PAUSE -> {
                val paused = intent.getBooleanExtra(
                    TaskNotificationControl.EXTRA_PAUSED,
                    false
                )
                ArchiveTaskControl.setPaused(context, taskId, paused)
            }
            TaskNotificationControl.ACTION_CANCEL -> {
                ArchiveTaskControl.clear(context, taskId)
                WorkManager.getInstance(context.applicationContext)
                    .cancelWorkById(taskId)
            }
        }
    }
}
