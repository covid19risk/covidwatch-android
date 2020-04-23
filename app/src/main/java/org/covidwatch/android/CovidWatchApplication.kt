package org.covidwatch.android

import android.app.Application
import org.covidwatch.android.data.contactevent.ContactEventsDownloader
import org.covidwatch.android.data.contactevent.LocalContactEventsUploader
import org.covidwatch.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.android.ext.android.inject

class CovidWatchApplication : Application() {

    private lateinit var localContactEventsUploader: LocalContactEventsUploader
    private val contactEventsDownloader: ContactEventsDownloader by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(applicationContext)
            modules(appModule)
        }

        localContactEventsUploader = LocalContactEventsUploader(this)
        localContactEventsUploader.startUploading()

        contactEventsDownloader.schedulePeriodicPublicContactEventsRefresh()
    }
}
