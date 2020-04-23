package org.covidwatch.android.data.contactevent

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.covidwatch.android.R
import org.covidwatch.android.data.ContactEventDAO
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.data.contactevent.firebase.FirebaseContactEventFetcher
import org.covidwatch.android.domain.NotifyAboutPossibleExposureUseCase
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*
import java.util.concurrent.TimeUnit

class ContactEventsDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams),
    KoinComponent {

    companion object {
        private const val TAG = "ContactEventsDownloadWorker"

        // Only fetch contact events from the past 2 weeks
        private val OLDEST_PUBLIC_CONTACT_EVENTS_TO_FETCH_MILLIS =
            TimeUnit.SECONDS.toMillis(60 * 60 * 24 * 7 * 2)

        private val lastFetchTime get() =
            Date().apply { time -= OLDEST_PUBLIC_CONTACT_EVENTS_TO_FETCH_MILLIS }
    }

    // TODO: Get ContactEventFetcher via DI
    private val contactEventFetcher: ContactEventFetcher = FirebaseContactEventFetcher(context)

    private val notifyAboutPossibleExposureUseCase: NotifyAboutPossibleExposureUseCase by inject()

    override fun doWork(): Result {

        Log.i(TAG, "Downloading contact events")

        val lastFetchTime = lastFetchTime
        var fetchSinceTime = context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).getLong(
            context.getString(R.string.preference_last_contact_events_download_date),
            lastFetchTime.time
        )
        if (fetchSinceTime < lastFetchTime.time) {
            fetchSinceTime = lastFetchTime.time
        }

        try {
            contactEventFetcher.fetch(Date(fetchSinceTime)..Date()) { infectionState ->
                markLocalContactEventsWith(infectionState)
            }
            notifyAboutPossibleExposureUseCase.execute()

            return Result.success()
        } catch (ex: Exception) {
            Log.e(TAG, "Error fetching contact events", ex)
            return Result.failure()
        } finally {
            Log.i(TAG, "Finish task")
        }
    }

    private fun List<String>.markLocalContactEventsWith(infectionState: InfectionState) {
        Log.d(TAG, "Marking ${size} contact event(s) as $infectionState ...")
        val dao: ContactEventDAO = CovidWatchDatabase.getInstance(context).contactEventDAO()
        val chunkSize = 998 // SQLITE_MAX_VARIABLE_NUMBER - 1
        chunked(chunkSize).forEach {
            dao.update(it, infectionState.isPotentiallyInfectious)
            Log.d(TAG, "Marked ${it.size} contact event(s) as $infectionState")
        }
    }
}

