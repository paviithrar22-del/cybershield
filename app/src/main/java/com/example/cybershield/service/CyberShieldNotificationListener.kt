package com.example.cybershield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.cybershield.MainActivity
import com.example.cybershield.data.AppSettings
import com.example.cybershield.data.Incident
import com.example.cybershield.data.IncidentDatabaseHelper
import com.example.cybershield.nlp.BullyingClassifier
import java.util.Locale
import kotlinx.coroutines.launch

class CyberShieldNotificationListener : NotificationListenerService() {

    private val TAG = "CyberShieldService"
    private val CHANNEL_ID = "cybershield_alerts"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "CyberShield notification listener service created.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName ?: return
        val appSettings = AppSettings.getInstance(applicationContext)

        // Check if package is monitored
        val monitoredApps = appSettings.monitoredApps
        val isMonitored = monitoredApps.any { it.lowercase(Locale.ROOT) == packageName.lowercase(Locale.ROOT) }
        
        if (!isMonitored) {
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Someone"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val content = text.ifEmpty { bigText }

        if (content.isEmpty()) {
            return
        }

        // Avoid classifying own app notifications
        if (packageName == applicationContext.packageName) {
            return
        }

        // Increment scanned count
        appSettings.incrementScanned()

        // Classify and handle result in background scope
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val apiKey = com.example.cybershield.nlp.GeminiClassifier.API_KEY
            val result = if (apiKey.isNotBlank() && apiKey != "YOUR_API_KEY_HERE") {
                com.example.cybershield.nlp.GeminiClassifier.classify(content, apiKey)
            } else {
                com.example.cybershield.data.SupabaseManager.getInstance().classifyWithGeminiEdge(content)
            }

            if (result.isFlagged) {
                // Increment flagged count
                appSettings.incrementFlagged()

                // Save incident to DB
                val incident = Incident(
                    timestamp = System.currentTimeMillis(),
                    sender = title,
                    messageContent = content,
                    sourceApp = packageName,
                    severity = result.severity,
                    reason = result.reason
                )
                IncidentDatabaseHelper.getInstance(applicationContext).insertIncident(incident)

                // Sync to Supabase in background
                try {
                    com.example.cybershield.data.SupabaseManager.getInstance().syncIncident(incident)
                } catch (e: Exception) {
                    android.util.Log.e("CyberShieldService", "Failed to sync incident to Supabase", e)
                }

                // Trigger Alert
                val appLabel = getAppName(packageName)
                sendLocalAlert(title, content, appLabel, result.severity)
                
                val guardianPhone = appSettings.guardianPhone
                if (guardianPhone.isNotEmpty()) {
                    sendGuardianSms(guardianPhone, appSettings.guardianName, title, content, appLabel, result.severity)
                }
            }
        }
    }

    private fun getAppName(packageName: String): String {
        val pm = packageManager
        return try {
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName.split(".").lastOrNull()
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } 
                ?: packageName
        }
    }

    private fun sendLocalAlert(sender: String, content: String, appLabel: String, severity: com.example.cybershield.nlp.Severity) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val severityEmoji = when(severity) {
            com.example.cybershield.nlp.Severity.CRITICAL -> "🚨 CRITICAL"
            com.example.cybershield.nlp.Severity.HIGH -> "⚠️ HIGH"
            else -> "⚡ MEDIUM"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("CyberShield Flagged Message ($severityEmoji)")
            .setContentText("Detected from $sender on $appLabel")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Sender: $sender\nApp: $appLabel\nMessage: \"$content\""
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun sendGuardianSms(phone: String, guardianName: String, sender: String, content: String, appLabel: String, severity: com.example.cybershield.nlp.Severity) {
        if (checkCallingOrSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot send Guardian SMS: SEND_SMS permission is not granted.")
            return
        }

        val smsMessage = "CyberShield Alert: [Severity: ${severity.name}] Message flagged from '$sender' on $appLabel: \"$content\""
        
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            // If the message is too long, divide it
            val parts = smsManager.divideMessage(smsMessage)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phone, null, smsMessage, null, null)
            }
            Log.d(TAG, "Guardian alert SMS sent successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Guardian SMS alert", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "CyberShield Incident Alerts"
            val descriptionText = "Notifications sent when potential cyberbullying or harassment is detected."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
