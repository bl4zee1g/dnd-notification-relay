package dev.notificationrelay

import android.app.Notification
import android.app.NotificationManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RelayListener : NotificationListenerService() {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Fingerprint of (package, title, body) -> time it was last forwarded. */
    private val recentlySent = object : LinkedHashMap<Int, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Long>?): Boolean =
            size > 500
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName == packageName) return
        if (sbn.packageName == DISCORD_PACKAGE) return
        if (!isDoNotDisturbActive()) return

        val webhook = Prefs.webhook(this)
        if (webhook.isEmpty()) return

        val notification = sbn.notification ?: return
        if (isSilent(sbn)) return

        val title = notification.extras
            .getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        var body = notification.extras
            .getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (body.isEmpty()) {
            body = notification.extras
                .getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim().orEmpty()
        }
        if (title.isEmpty() && body.isEmpty()) return

        // Apps like WhatsApp post one message under several keys; dedupe on content, not key.
        val fingerprint = "${sbn.packageName}\u0000$title\u0000$body".hashCode()
        val now = SystemClock.elapsedRealtime()
        synchronized(recentlySent) {
            val last = recentlySent[fingerprint]
            if (last != null && now - last < DEDUPE_WINDOW_MS) return
            recentlySent[fingerprint] = now
        }

        val label = appLabel(sbn.packageName)
        executor.execute { DiscordWebhook.send(webhook, label, title, body) }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun isDoNotDisturbActive(): Boolean =
        when (currentInterruptionFilter) {
            INTERRUPTION_FILTER_PRIORITY,
            INTERRUPTION_FILTER_ALARMS,
            INTERRUPTION_FILTER_NONE -> true
            else -> false
        }

    private fun isSilent(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification ?: return true

        val ranking = NotificationListenerService.Ranking()
        val ranked = currentRanking?.getRanking(sbn.key, ranking) == true

        if (ranked) {
            val channelImportance = ranking.channel?.importance
            if (channelImportance != null) {
                return channelImportance <= NotificationManager.IMPORTANCE_LOW
            }
            if (ranking.importance >= NotificationManager.IMPORTANCE_MIN) {
                return ranking.importance <= NotificationManager.IMPORTANCE_LOW
            }
        }

        // No ranking info: look the channel up on the listener's side.
        val channelId = notification.channelId
        if (channelId != null) {
            val channel = try {
                getNotificationChannels(sbn.packageName, sbn.user)
                    ?.firstOrNull { it.id == channelId }
            } catch (_: Exception) {
                null
            }
            if (channel != null) {
                return channel.importance <= NotificationManager.IMPORTANCE_LOW
            }
        }

        val noSound = notification.sound == null &&
            (notification.defaults and Notification.DEFAULT_SOUND) == 0
        val noVibrate = notification.vibrate == null &&
            (notification.defaults and Notification.DEFAULT_VIBRATE) == 0
        return noSound && noVibrate
    }

    private fun appLabel(pkg: String): String = try {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        pkg
    }

    private companion object {
        const val DISCORD_PACKAGE = "com.discord"
        const val DEDUPE_WINDOW_MS = 60_000L
    }
}
