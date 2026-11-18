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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webhookLayout: TextInputLayout
    private lateinit var webhookInput: TextInputEditText
    private lateinit var accessStatus: TextView
    private lateinit var excludedStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webhookLayout = findViewById(R.id.webhookLayout)
        webhookInput = findViewById(R.id.webhookInput)
        accessStatus = findViewById(R.id.accessStatus)
        excludedStatus = findViewById(R.id.excludedStatus)

        webhookInput.doAfterTextChanged { webhookLayout.error = null }

        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener { saveWebhook() }
        findViewById<MaterialButton>(R.id.grantButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<MaterialButton>(R.id.chooseAppsButton).setOnClickListener { chooseExcludedApps() }
    }

    override fun onResume() {
        super.onResume()
        if (!webhookInput.hasFocus()) {
            webhookInput.setText(Prefs.webhook(this))
        }
        updateAccessStatus()
        updateExcludedStatus()
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

    private fun updateExcludedStatus() {
        val count = Prefs.excludedPackages(this).size
        excludedStatus.text = getString(R.string.excluded_status, count)
    }

    private fun chooseExcludedApps() {
        val apps = launchableApps()
        val excluded = Prefs.excludedPackages(this)
        val checked = BooleanArray(apps.size) { apps[it].second in excluded }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.excluded_title)
            .setMultiChoiceItems(apps.map { it.first }.toTypedArray(), checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(R.string.save) { _, _ ->
                // Keep excluded packages that aren't in the launcher list (e.g. uninstalled apps).
                val listed = apps.map { it.second }.toSet()
                val selected = apps.filterIndexed { i, _ -> checked[i] }.map { it.second }
                Prefs.saveExcludedPackages(this, (excluded - listed) + selected)
                updateExcludedStatus()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun launchableApps(): List<Pair<String, String>> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcher, 0)
            .map { it.loadLabel(packageManager).toString() to it.activityInfo.packageName }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }
    }
}
