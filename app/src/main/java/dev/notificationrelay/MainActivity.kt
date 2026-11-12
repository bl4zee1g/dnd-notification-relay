package dev.notificationrelay

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webhookLayout: TextInputLayout
    private lateinit var webhookInput: TextInputEditText
    private lateinit var accessStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webhookLayout = findViewById(R.id.webhookLayout)
        webhookInput = findViewById(R.id.webhookInput)
        accessStatus = findViewById(R.id.accessStatus)

        webhookInput.doAfterTextChanged { webhookLayout.error = null }

        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener { saveWebhook() }
        findViewById<MaterialButton>(R.id.grantButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!webhookInput.hasFocus()) {
            webhookInput.setText(Prefs.webhook(this))
        }
        updateAccessStatus()
    }

    private fun updateAccessStatus() {
        val granted = hasNotificationAccess()
        accessStatus.setText(
            if (granted) R.string.access_granted else R.string.access_missing
        )
        accessStatus.setTextColor(
            MaterialColors.getColor(
                accessStatus,
                if (granted) {
                    com.google.android.material.R.attr.colorPrimary
                } else {
                    com.google.android.material.R.attr.colorError
                }
            )
        )
    }

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val component = ComponentName(this, RelayListener::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == component }
    }

    private fun saveWebhook() {
        val url = webhookInput.text?.toString()?.trim().orEmpty()
        if (!DiscordWebhook.isValidUrl(url)) {
            webhookLayout.error = getString(R.string.invalid_webhook)
            return
        }
        webhookLayout.error = null
        Prefs.saveWebhook(this, url)

        webhookInput.clearFocus()
        getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(webhookInput.windowToken, 0)
        Snackbar.make(webhookInput, R.string.saved, Snackbar.LENGTH_SHORT).show()
    }
}
