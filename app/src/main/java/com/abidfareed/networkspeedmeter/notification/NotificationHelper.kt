package com.abidfareed.networkspeedmeter.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.abidfareed.networkspeedmeter.MainActivity
import com.abidfareed.networkspeedmeter.R
import com.abidfareed.networkspeedmeter.settings.DisplayMode
import com.abidfareed.networkspeedmeter.settings.TextSize

/**
 * Builds the single ongoing, silent, low-importance notification whose small icon is a
 * live-rendered bitmap of the current speed (see [SpeedIconRenderer]). Kept deliberately
 * minimal: no sound, no vibration, no heads-up, no expandable content, not swipe-dismissible
 * (required for a live foreground-service indicator) - the practical floor Android allows for
 * a foreground service notification, per TECHNICAL_FEASIBILITY.md.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "speed_meter_status"
        const val NOTIFICATION_ID = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        downloadBytesPerSec: Long,
        uploadBytesPerSec: Long,
        contentText: String,
        iconBitmap: Bitmap
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(IconCompat.createWithBitmap(iconBitmap))
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(contentIntent)
            .setShowWhen(false)
            .build()
    }

    fun iconForSpeeds(
        downloadBytesPerSec: Long,
        uploadBytesPerSec: Long,
        displayMode: DisplayMode,
        formattedDown: Pair<String, String>,
        formattedUp: Pair<String, String>,
        textSize: TextSize = TextSize.MEDIUM,
        decimalPlaces: Int = 0
    ): Bitmap {
        val scale = when (textSize) {
            TextSize.SMALL -> 0.8f
            TextSize.MEDIUM -> 1.0f
            TextSize.LARGE -> 1.2f
        }
        return when (displayMode) {
            DisplayMode.DOWNLOAD_ONLY -> SpeedIconRenderer.render(
                value = formattedDown.first,
                unit = formattedDown.second,
                decimalPlaces = decimalPlaces,
                sizeScale = scale
            )
            DisplayMode.UPLOAD_ONLY -> SpeedIconRenderer.render(
                value = formattedUp.first,
                unit = formattedUp.second,
                decimalPlaces = decimalPlaces,
                sizeScale = scale
            )
            DisplayMode.DOWNLOAD_AND_UPLOAD -> SpeedIconRenderer.renderCombined(
                downValue = formattedDown.first,
                downUnit = formattedDown.second,
                upValue = formattedUp.first,
                upUnit = formattedUp.second,
                decimalPlaces = decimalPlaces,
                sizeScale = scale
            )
        }
    }
}
