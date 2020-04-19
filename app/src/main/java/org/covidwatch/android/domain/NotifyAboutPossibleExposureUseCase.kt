package org.covidwatch.android.domain

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationManagerCompat
import org.covidwatch.android.NotificationFactory
import org.covidwatch.android.data.ContactEventDAO

class NotifyAboutPossibleExposureUseCase(
    private val context: Context,
    private val notificationFactory: NotificationFactory,
    private val testedRepository: TestedRepository,
    private val contactEventDAO: ContactEventDAO
) {

    @WorkerThread
    fun execute() {
        if (testedRepository.isUserTestedPositive()) return

        val contactEvents = contactEventDAO.all
        if (contactEvents.any { it.wasPotentiallyInfectious }) {
            showNotification()
        }
    }

    private fun showNotification() {
        val notification = notificationFactory.possibleExposure()
        NotificationManagerCompat.from(context).notify(0, notification)
    }
}