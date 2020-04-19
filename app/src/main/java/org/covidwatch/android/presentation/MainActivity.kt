package org.covidwatch.android.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.covidwatch.android.BuildConfig
import org.covidwatch.android.R
import org.covidwatch.android.data.contactevent.ContactEventFetcher
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val contactEventFetcher: ContactEventFetcher by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (BuildConfig.DEBUG) {
            contactEventFetcher.startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (BuildConfig.DEBUG) {
            contactEventFetcher.stopListening()
        }
    }
}
