package dev.notificationrelay

import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DiscordWebhook {

    private const val MAX_CONTENT = 1900

    fun isValidUrl(value: String): Boolean {
        val uri = Uri.parse(value)
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        if (host !in setOf("discord.com", "discordapp.com", "ptb.discord.com", "canary.discord.com")) {
            return false
        }
        return uri.path?.startsWith("/api/webhooks/") == true
    }

    fun send(webhookUrl: String, app: String, title: String, text: String) {
        val message = buildString {
            append("**").append(app).append("**")
            if (title.isNotEmpty()) append('\n').append(title)
            if (text.isNotEmpty()) append('\n').append(text)
        }.take(MAX_CONTENT)

        val body = JSONObject().put("content", message).toString()
        try {
            val connection = URL(webhookUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "NotificationRelay/1.0")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            connection.responseCode
            connection.disconnect()
        } catch (_: Exception) {
            // Best effort: a failed relay must not affect the notification listener.
        }
    }
}
