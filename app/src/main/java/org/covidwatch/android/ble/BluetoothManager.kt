package org.covidwatch.android.ble

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import org.covidwatch.android.R
import org.covidwatch.android.data.ContactEvent
import org.covidwatch.android.data.ContactEventDAO
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.presentation.MainActivity
import org.tcncoalition.tcnclient.BluetoothService
import org.tcncoalition.tcnclient.BluetoothService.LocalBinder
import org.tcncoalition.tcnclient.cen.*
import org.tcncoalition.tcnclient.toBytes
import org.tcncoalition.tcnclient.toUUID
import java.util.*
import java.util.concurrent.TimeUnit


interface BluetoothManager {
    fun startAdvertiser(cen: Cen)
    fun stopAdvertiser()

    fun startService(cen: Cen)
    fun stopService()

    fun changeAdvertisedValue(cen: Cen)
}

class BluetoothManagerImpl(
    private val app: Application
) : BluetoothManager {

    private val intent get() = Intent(app, BluetoothService::class.java)

    private var service: BluetoothService? = null

    private val cenGenerator = DefaultCenGenerator()
    private val cenVisitor = DefaultCenVisitor(app)
    private var timer: Timer? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@BluetoothManagerImpl.service = (service as LocalBinder).service.apply {
                configure(
                    BluetoothService.ServiceConfiguration(
                        cenGenerator,
                        cenVisitor,
                        foregroundNotification()
                    )
                )
                if (BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser != null) start()
            }

            runTimer()
        }

        override fun onServiceDisconnected(name: ComponentName?) = Unit
    }

    private fun runTimer() {
        // scheduler a new timer to start changing the contact event numbers
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    service?.updateCen()
                }
            },
            TimeUnit.MINUTES.toMillis(CEN_CHANGE_INTERVAL_MIN),
            TimeUnit.MINUTES.toMillis(CEN_CHANGE_INTERVAL_MIN)
        )
    }

    private fun foregroundNotification(): Notification {
        createNotificationChannelIfNeeded()

        val notificationIntent = Intent(app, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            app, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(app, CHANNEL_ID)
            .setContentTitle(app.getString(R.string.foreground_notification_title))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    override fun changeAdvertisedValue(cen: Cen) {
        cenGenerator.cen = cen
        service?.updateCen()
    }

    override fun startAdvertiser(cen: Cen) {
        cenGenerator.cen = cen
        service?.startAdvertiser()
    }

    override fun startService(cen: Cen) {
        cenGenerator.cen = cen
        app.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(intent)
        else app.startService(intent)
    }

    override fun stopAdvertiser() {
        service?.stopAdvertiser()
    }

    override fun stopService() {
        service?.stopAdvertiser()
        app.stopService(intent)
    }

    /**
     * This notification channel is only required for android versions above
     * android O. This creates the necessary notification channel for the foregroundService
     * to function.
     */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                app, NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    inner class DefaultCenVisitor(private val ctx: Context) : CenVisitor {
        private fun handleCEN(cen: Cen) {
            Log.i(TAG, "Running handleCEN")
            CovidWatchDatabase.databaseWriteExecutor.execute {
                val dao: ContactEventDAO = CovidWatchDatabase.getInstance(ctx).contactEventDAO()
                val contactEvent = ContactEvent(cen.data.toUUID().toString())
                val isCurrentUserSick = ctx.getSharedPreferences(
                    ctx.getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                ).getBoolean(ctx.getString(R.string.preference_is_current_user_sick), false)
                contactEvent.wasPotentiallyInfectious = isCurrentUserSick
                dao.insert(contactEvent)
            }
        }

        override fun visit(cen: GeneratedCen) {
            Log.i(TAG, "Handling generated CEN")
            this.handleCEN(cen)
        }

        override fun visit(cen: ObservedCen) {
            Log.i(TAG, "Handling observed CEN")
            this.handleCEN(cen)
        }
    }

    class DefaultCenGenerator(var cen: Cen? = null) : CenGenerator {
        override fun generate(): GeneratedCen {
            return   GeneratedCen(UUID.randomUUID().toBytes())
        }
    }

    companion object {
        // CONSTANTS
        private const val CEN_CHANGE_INTERVAL_MIN = 15L
        private const val CHANNEL_ID = "CovidBluetoothContactChannel"
        private const val TAG = "BluetoothManagers"
    }
}
