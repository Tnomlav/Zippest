package com.decompress.app.data

import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit

object OpenWithPreferences {
    private const val PREFERENCES = "zippest-open-with"

    fun get(context: Context, mimeType: String, extension: String): ComponentName? {
        val value = prefs(context).getString(key(mimeType, extension), null) ?: return null
        return ComponentName.unflattenFromString(value)
    }

    fun set(
        context: Context,
        mimeType: String,
        extension: String,
        component: ComponentName
    ) {
        prefs(context).edit {
            putString(key(mimeType, extension), component.flattenToString())
        }
    }

    fun clear(context: Context) {
        prefs(context).edit { clear() }
    }

    private fun key(mimeType: String, extension: String): String =
        "${mimeType.lowercase()}::${extension.lowercase()}"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFERENCES,
            Context.MODE_PRIVATE
        )
}
