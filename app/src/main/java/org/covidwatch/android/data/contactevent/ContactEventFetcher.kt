package org.covidwatch.android.data.contactevent

import java.util.*

/**
 * Asynchronously fetch contact events from the backend.
 */
interface ContactEventFetcher {

    /**
     * Asynchronously fetch contact events from the backend.
     * @param timeWindow The time window to fetch the contact events.
     * @param resultCb Callback for receiving the contact events with the given IDs with the given
     * infection state. May be called more than once per method call.
     * @return The resulting contact events.
     * @exception Exception Throws an exception on error.
     */
    fun fetch(timeWindow: ClosedRange<Date>)

    /**
     * Start listening for contact events from the backend.
     * @exception Exception Throws an exception on error.
     */
    fun startListening()

    /**
     * Stop listening for contact events from the backend.
     */
    fun stopListening()
}