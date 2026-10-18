package com.cybertran.enquadraplus.data.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cybertran.enquadraplus.MainActivity

object UpdateNotificationManager {
    const val CHANNEL_ID = "normative_updates"
    private const val CHANNEL_NAME = "Atualizações jurídicas"
    private const val NOTIFICATION_ID = 4101
    private const val PREFS_NAME = "normative_update_notifications"
    private const val LAST_FINGERPRINT = "last_candidate_fingerprint"

    fun notificationsAllowed(context: Context): Boolean {
        val permissionAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

        return permissionAllowed &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun notifyValidatedCandidates(
        context: Context,
        states: List<DatasetUpdateState>,
    ) {
        val candidates = states
            .filter { it.canApply }
            .sortedBy { it.dataset.id }

        if (candidates.isEmpty() || !notificationsAllowed(context)) return

        val fingerprint = candidates.joinToString("|") {
            it.dataset.id + ":" + it.candidateVersion.orEmpty()
        }
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (preferences.getString(LAST_FINGERPRINT, null) == fingerprint) return

        createChannel(context)

        val title = if (candidates.size == 1) {
            "Atualização jurídica disponível"
        } else {
            "Atualizações jurídicas disponíveis"
        }
        val names = candidates.joinToString(", ") { it.dataset.displayName }
        val message = "Candidata validada pronta para decisão: $names."

        val openApp = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        preferences.edit().putString(LAST_FINGERPRINT, fingerprint).apply()
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisa quando uma candidata normativa validada está disponível."
        }
        manager.createNotificationChannel(channel)
    }
}
