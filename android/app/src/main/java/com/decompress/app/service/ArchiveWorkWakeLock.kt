package com.decompress.app.service

import android.content.Context
import android.os.PowerManager

internal object ArchiveWorkWakeLock {
    private const val TIMEOUT_MILLIS = 12L * 60L * 60L * 1000L

    fun acquire(context: Context, taskId: String): PowerManager.WakeLock {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Zippest:archive:$taskId"
        ).apply {
            setReferenceCounted(false)
            acquire(TIMEOUT_MILLIS)
        }
    }
}
