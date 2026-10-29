package com.decompress.app.service

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

object ArchiveTaskControl {
    private const val PREFERENCES = "archive-task-controls"
    private const val PAUSED_PREFIX = "paused:"

    fun isPaused(context: Context, taskId: UUID): Boolean =
        prefs(context).getBoolean(PAUSED_PREFIX + taskId, false)

    fun setPaused(context: Context, taskId: UUID, paused: Boolean) {
        prefs(context).edit { putBoolean(PAUSED_PREFIX + taskId, paused) }
    }

    fun clear(context: Context, taskId: UUID) {
        prefs(context).edit { remove(PAUSED_PREFIX + taskId) }
    }

    fun waitWhilePaused(
        context: Context,
        taskId: UUID,
        isCancelled: () -> Boolean,
        onPaused: () -> Unit
    ): Boolean {
        if (!isPaused(context, taskId)) return true
        onPaused()
        while (isPaused(context, taskId)) {
            if (isCancelled()) return false
            Thread.sleep(250)
        }
        return true
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFERENCES,
            Context.MODE_PRIVATE
        )
}
