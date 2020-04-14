package org.covidwatch.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.covidwatch.android.ble.BluetoothManagerImpl
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.data.firestore.ContactEventsDownloadWorker
import org.covidwatch.android.data.firestore.LocalContactEventsUploader
import org.tcncoalition.tcnclient.cen.GeneratedCen
import org.tcncoalition.tcnclient.toBytes
import java.util.*
import java.util.concurrent.TimeUnit


class CovidWatchApplication : Application() {

    private lateinit var localContactEventsUploader: LocalContactEventsUploader
    private lateinit var currentUserExposureNotifier: CurrentUserExposureNotifier
    private val mainScope = CoroutineScope(Dispatchers.Main)
    //TODO: Move to DI module
    private val bluetoothManager = BluetoothManagerImpl(this)

    private var sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                getString(R.string.preference_is_current_user_sick) -> {
                    val isCurrentUserSick = sharedPreferences.getBoolean(
                        getString(R.string.preference_is_current_user_sick),
                        false
                    )
                    Log.i(TAG, "Current user is sick=$isCurrentUserSick")
                    if (isCurrentUserSick) {
                        Log.i(TAG, "Marking all local contact events as potentially infectious...")
                        CovidWatchDatabase.databaseWriteExecutor.execute {
                            CovidWatchDatabase.getInstance(this)
                                .contactEventDAO()
                                .markAllAsPotentiallyInfectious()
                            Log.i(TAG, "Marked all local contact events as potentially infectious")
                        }
                    }
                }
                getString(R.string.preference_is_contact_event_logging_enabled) -> {
                    val isContactEventLoggingEnabled = sharedPreferences.getBoolean(
                        getString(R.string.preference_is_contact_event_logging_enabled),
                        false
                    )
                    configureAdvertising(isContactEventLoggingEnabled)
                }
            }
        }

    private fun configureAdvertising(enabled: Boolean) {
        if (enabled) {
            bluetoothManager.startService(GeneratedCen(UUID.randomUUID().toBytes()))
        } else {
            bluetoothManager.stopService()
        }
    }

    override fun onCreate() {
        super.onCreate()

        getSharedPreferences(
            getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        localContactEventsUploader = LocalContactEventsUploader(this)
        localContactEventsUploader.startUploading()

        schedulePeriodicPublicContactEventsRefresh()

        currentUserExposureNotifier =
            CurrentUserExposureNotifier(this)
        currentUserExposureNotifier.startObservingLocalContactEvents()

        val isContactEventLoggingEnabled = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        ).getBoolean(
            getString(R.string.preference_is_contact_event_logging_enabled),
            false
        )
        configureAdvertising(isContactEventLoggingEnabled)


    }

    private fun schedulePeriodicPublicContactEventsRefresh() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest =
            PeriodicWorkRequestBuilder<ContactEventsDownloadWorker>(5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ContactEventsDownloadWorker.WORKER_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            downloadRequest
        )
    }

    companion object {
        private const val TAG = "CovidWatchApplication"
    }
}
