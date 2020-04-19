package org.covidwatch.android.data.contactevent.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.ListenableWorker
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import org.covidwatch.android.R
import org.covidwatch.android.data.ContactEventDAO
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.data.contactevent.ContactEventFetcher
import org.covidwatch.android.data.contactevent.InfectionState
import org.covidwatch.android.domain.NotifyAboutPossibleExposureUseCase
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "ResultFetcher"

/**
 * Firebase implementation of the ContactEventFetcher interface.
 */
class FirebaseContactEventFetcher(
    private val context: Context,
    private val preferences: SharedPreferences,
    private val contactEventDAO: ContactEventDAO,
    private val notifyAboutPossibleExposureUseCase: NotifyAboutPossibleExposureUseCase
) : ContactEventFetcher {

    private var listenerRegistration: ListenerRegistration? = null

    override fun fetch(
        timeWindow: ClosedRange<Date>
    ) {
        val task = FirebaseFirestore.getInstance()
            .collection(FirestoreConstants.COLLECTION_CONTACT_EVENTS)
            .whereGreaterThan(
                FirestoreConstants.FIELD_TIMESTAMP,
                Timestamp(timeWindow.start)
            )
            .get()
            .continueWith(
                CovidWatchDatabase.databaseWriteExecutor,
                Continuation<QuerySnapshot, Unit> {
                    it.result?.handleQueryResult(timeWindow)
                })

        Tasks.await(task)
        checkResultForErrors(task)
    }

    override fun startListening() {
        val fetchSinceTime = Date().apply { time -= TimeUnit.DAYS.toMillis(14) }
        val timeWindow = Date(fetchSinceTime.time)..Date()

        listenerRegistration = FirebaseFirestore.getInstance()
            .collection(FirestoreConstants.COLLECTION_CONTACT_EVENTS)
            .whereGreaterThan(
                FirestoreConstants.FIELD_TIMESTAMP,
                Timestamp(timeWindow.start)
            )
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || snapshot.isEmpty) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                CovidWatchDatabase.databaseWriteExecutor.execute {
                    snapshot.handleQueryResult(timeWindow)
                    notifyAboutPossibleExposureUseCase.execute()
                }
            }
    }

    override fun stopListening() {
        listenerRegistration?.remove()
    }

    private fun checkResultForErrors(task: Task<Unit>) {
        if (!task.isSuccessful)
            throw task.exception ?: RuntimeException("Could not fetch contacts")
    }

    private fun QuerySnapshot.handleQueryResult(
        timeWindow: ClosedRange<Date>
    ) {
        Log.i(
            TAG,
            "Downloaded ${size()} contact event(s)"
        )
        try {
            val addedDocumentChanges =
                documentChanges.filter {
                    it.type == DocumentChange.Type.ADDED
                }
            val removedDocumentChanges =
                documentChanges.filter {
                    it.type == DocumentChange.Type.REMOVED
                }
            addedDocumentChanges.markLocalContactEvents {
                markLocalContactEventsWith(InfectionState.PotentiallyInfectious)
            }
            removedDocumentChanges.markLocalContactEvents {
                markLocalContactEventsWith(InfectionState.Healthy)
            }

            preferences
                .edit()
                .putLong(
                    context.getString(R.string.preference_last_contact_events_download_date),
                    timeWindow.endInclusive.time
                )
                .apply()

            ListenableWorker.Result.success()

        } catch (exception: Exception) {
            ListenableWorker.Result.failure()
        }
    }

    private fun List<DocumentChange>.markLocalContactEvents(
        markLocalContactEvents: List<String>.() -> Unit
    ) {
        if (isEmpty()) return
        val identifiers = map { it.document.id }
        identifiers.markLocalContactEvents()
    }

    private fun List<String>.markLocalContactEventsWith(infectionState: InfectionState) {
        Log.d(TAG, "Marking ${size} contact event(s) as $infectionState ...")
        val chunkSize = 998 // SQLITE_MAX_VARIABLE_NUMBER - 1
        chunked(chunkSize).forEach {
            contactEventDAO.update(it, infectionState.isPotentiallyInfectious)
            Log.d(TAG, "Marked ${it.size} contact event(s) as $infectionState")
        }
    }
}