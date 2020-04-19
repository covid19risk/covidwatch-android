package org.covidwatch.android

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat

internal const val CHANNEL_ID = "CurrentUserExposureNotifier"

class NotificationFactory(
    private val context: Context
) {

    fun possibleExposure(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentText(context.getString(R.string.notification_current_user_was_exposed))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}