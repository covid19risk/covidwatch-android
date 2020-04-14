package org.covidwatch.android.data.firestore

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import org.covidwatch.android.R
import org.covidwatch.android.data.ContactEventDAO
import org.covidwatch.android.data.CovidWatchDatabase
import java.util.*

class ContactEventsDownloadWorker(var context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        private const val TAG = "ContactEventsDownloadWorker"
        const val WORKER_NAME = "org.covidwatch.android.refresh"
    }

    private var result = Result.failure()

    override fun doWork(): Result {

        Log.i(TAG, "Downloading contact events")

        val now = Date()

        val lastFetchTime = FirestoreConstants.lastFetchTime()
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

        val task =
            FirebaseFirestore.getInstance().collection(FirestoreConstants.COLLECTION_CONTACT_EVENTS)
                .whereGreaterThan(
                    FirestoreConstants.FIELD_TIMESTAMP,
                    Timestamp(Date(fetchSinceTime))
                )
                .get()
                .continueWith(
                    CovidWatchDatabase.databaseWriteExecutor,
                    Continuation<QuerySnapshot, Result> { task ->
                        //result = Result.success()
                        val queryDocumentSnapshots = task.result
                        if (queryDocumentSnapshots != null) {
                            Log.i(
                                TAG,
                                "Downloaded ${queryDocumentSnapshots.size()} contact event(s)"
                            )
                            result = try {
                                val addedDocumentChanges =
                                    queryDocumentSnapshots.documentChanges.filter {
                                        it.type == DocumentChange.Type.ADDED
                                    }
                                val removedDocumentChanges =
                                    queryDocumentSnapshots.documentChanges.filter {
                                        it.type == DocumentChange.Type.REMOVED
                                    }
                                markLocalContactEvents(addedDocumentChanges, true)
                                markLocalContactEvents(removedDocumentChanges, false)

                                with(
                                    context.getSharedPreferences(
                                        context.getString(R.string.preference_file_key),
                                        Context.MODE_PRIVATE
                                    ).edit()
                                ) {
                                    putLong(
                                        context.getString(R.string.preference_last_contact_events_download_date),
                                        now.time
                                    )
                                    commit()
                                }

                                Result.success()

                            } catch (exception: Exception) {
                                Result.failure()
                            }
                        } else {
                            result = Result.failure()
                        }
                        null
                    })

         Tasks.await(task)

        Log.i(TAG, "Finish task")

        return result
    }

    private fun markLocalContactEvents(
        documentChanges: List<DocumentChange>,
        wasPotentiallyInfectious: Boolean
    ) {
        if (documentChanges.isEmpty()) return
        Log.d(
            TAG,
            "Marking ${documentChanges.size} contact event(s) as potentially infectious=$wasPotentiallyInfectious ..."
        )
        val dao: ContactEventDAO = CovidWatchDatabase.getInstance(context).contactEventDAO()
        val identifiers = documentChanges.map { it.document.id }
        val chunkSize = 998 // SQLITE_MAX_VARIABLE_NUMBER - 1
        identifiers.chunked(chunkSize).forEach {
            dao.update(it, wasPotentiallyInfectious)
            Log.d(
                TAG,
                "Marked ${it.size} contact event(s) as potentially infectious=$wasPotentiallyInfectious"
            )
        }
    }
}