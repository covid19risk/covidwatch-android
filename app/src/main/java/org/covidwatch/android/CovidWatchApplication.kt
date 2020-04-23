package org.covidwatch.android

import android.app.Application
import androidx.work.*
import org.covidwatch.android.data.contactevent.ContactEventsDownloadWorker
import org.covidwatch.android.data.contactevent.LocalContactEventsUploader
import org.covidwatch.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class CovidWatchApplication : Application() {

    private lateinit var localContactEventsUploader: LocalContactEventsUploader

    companion object {
        private var context: CovidWatchApplication? = null

        fun getContext(): CovidWatchApplication? {
            return context
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = this

        startKoin {
            androidContext(applicationContext)
            modules(appModule)
        }

        localContactEventsUploader = LocalContactEventsUploader(this)
        localContactEventsUploader.startUploading()

        schedulePeriodicPublicContactEventsRefresh()
    }

    private fun schedulePeriodicPublicContactEventsRefresh() {
        val downloadRequest =
            PeriodicWorkRequestBuilder<ContactEventsDownloadWorker>(1, TimeUnit.HOURS)
                .setConstraints(getRefreshConstraints())
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ContactEventsDownloadWorkerPeriodicRefresh",
            ExistingPeriodicWorkPolicy.REPLACE,
            downloadRequest
        )
    }

    fun executePublicContactEventsRefresh() {
        val downloadRequest = OneTimeWorkRequestBuilder<ContactEventsDownloadWorker>()
            .setConstraints(getRefreshConstraints())
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "ContactEventsDownloadWorkerOneTimeRefresh",
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )
    }

    private fun getRefreshConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
