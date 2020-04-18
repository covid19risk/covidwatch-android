package org.covidwatch.android.domain

import androidx.work.*
import org.covidwatch.android.data.contactevent.ContactEventsDownloadWorker

class RefreshPublicContactEventsUseCase(
    private val workManager: WorkManager
) {

    fun execute() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest =
            OneTimeWorkRequestBuilder<ContactEventsDownloadWorker>()
                .setConstraints(constraints)
                .build()

        workManager.enqueueUniqueWork(
            ContactEventsDownloadWorker.WORKER_NAME,
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )
    }
}