package org.covidwatch.android.ble

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.covidwatch.android.MainFragment.Companion.INITIAL_VISIT
import org.covidwatch.android.R
import org.covidwatch.android.ui.MainActivity
import java.util.*
import java.util.concurrent.TimeUnit

class EnableBluetoothService : IntentService("EnableBluetoothService") {

    private val CHANNEL_ID = "EnableBluetoothServiceChannel"
    private val NOTIF_ID = 15
    private val POLLING_INTERVAL = 5L
    private var timer: Timer? = null

    override fun onHandleIntent(intent: Intent?) {
        startPolling()
    }

    private fun startPolling(){
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    checkIfBluetoothIsOn()
                }
            },
            TimeUnit.MINUTES.toMillis(POLLING_INTERVAL),
            TimeUnit.MINUTES.toMillis(POLLING_INTERVAL)
        )
    }

    private fun checkIfBluetoothIsOn(){
        val initialVisit = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ).getBoolean(INITIAL_VISIT,false)
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if ((adapter == null || !adapter.isEnabled) && !initialVisit){
            NotificationManagerCompat.from(this).notify(NOTIF_ID,foregroundNotification())
        } else {
            NotificationManagerCompat.from(this).cancel(NOTIF_ID)
        }
    }

    private fun foregroundNotification(): Notification {
        createNotificationChannelIfNeeded()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(this.getString(R.string.enable_bluetooth_notification))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = ContextCompat.getSystemService(
                this, NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
