package dev.notificationrelay

import android.content.Context

object Prefs {

    private const val FILE = "relay"
    private const val KEY_WEBHOOK = "webhook"

    fun webhook(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_WEBHOOK, "")
            .orEmpty()

    fun saveWebhook(context: Context, url: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEBHOOK, url)
            .apply()
    }
}
