package org.covidwatch.android.data.contactevent

import androidx.work.*
import java.util.concurrent.TimeUnit

class ContactEventsDownloader(private val workManager: WorkManager) {

    fun schedulePeriodicPublicContactEventsRefresh() {
        val downloadRequest =
            PeriodicWorkRequestBuilder<ContactEventsDownloadWorker>(1, TimeUnit.HOURS)
                .setConstraints(getRefreshConstraints())
                .build()

        workManager.enqueueUniquePeriodicWork(
            "ContactEventsDownloadWorkerPeriodicRefresh",
            ExistingPeriodicWorkPolicy.REPLACE,
            downloadRequest
        )
    }

    fun executePublicContactEventsRefresh() {
        val downloadRequest = OneTimeWorkRequestBuilder<ContactEventsDownloadWorker>()
            .setConstraints(getRefreshConstraints())
            .build()

        workManager.enqueueUniqueWork(
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