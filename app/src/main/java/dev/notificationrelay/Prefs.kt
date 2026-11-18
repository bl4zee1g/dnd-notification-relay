package dev.notificationrelay

import android.content.Context

object Prefs {

    private const val FILE = "relay"
    private const val KEY_WEBHOOK = "webhook"
    private const val KEY_EXCLUDED = "excluded"
    private val DEFAULT_EXCLUDED: Set<String> = setOf("com.discord")

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

    fun excludedPackages(context: Context): Set<String> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getStringSet(KEY_EXCLUDED, DEFAULT_EXCLUDED)
            .orEmpty()

    fun saveExcludedPackages(context: Context, packages: Set<String>) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_EXCLUDED, packages)
            .apply()
    }
}
