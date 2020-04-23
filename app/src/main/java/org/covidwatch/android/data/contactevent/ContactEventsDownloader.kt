package org.covidwatch.android.data.contactevent

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import java.util.concurrent.TimeUnit

private const val PERIODIC_REFRESH = "ContactEventsDownloadWorkerPeriodicRefresh"
private const val ONE_TIME_REFRESH = "ContactEventsDownloadWorkerOneTimeRefresh"

class ContactEventsDownloader(private val workManager: WorkManager) {

    fun schedulePeriodicPublicContactEventsRefresh() {
        val downloadRequest =
            PeriodicWorkRequestBuilder<ContactEventsDownloadWorker>(1, TimeUnit.HOURS)
                .setConstraints(getRefreshConstraints())
                .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_REFRESH,
            ExistingPeriodicWorkPolicy.REPLACE,
            downloadRequest
        )
    }

    fun executePublicContactEventsRefresh(lifecycle: LifecycleOwner, callback: (success: Boolean) -> Unit) {
        val downloadRequest = OneTimeWorkRequestBuilder<ContactEventsDownloadWorker>()
            .setConstraints(getRefreshConstraints())
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_REFRESH,
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )

        workManager.getWorkInfosForUniqueWorkLiveData(ONE_TIME_REFRESH)
            .observe(lifecycle, Observer { workInfos ->
                val workInfo = workInfos.firstOrNull()
                if (workInfo != null) {
                    when {
                        workInfo.state == WorkInfo.State.SUCCEEDED -> callback(true)
                        workInfo.state == WorkInfo.State.FAILED -> callback(false)
                        workInfo.state == WorkInfo.State.CANCELLED -> callback(false)
                    }
                }
            })
    }

    private fun getRefreshConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}