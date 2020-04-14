package org.covidwatch.android.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import org.covidwatch.android.R
import org.covidwatch.android.data.ContactEvent
import org.covidwatch.android.data.CovidWatchDatabase

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val contactEvents =  CovidWatchDatabase.getInstance(application)
        .contactEventDAO()
        .pagedAllSortedByDescTimestamp
        .toLiveData(pageSize = 50)

    private val _hasPossiblyInteractedWithInfected: LiveData<Boolean> = Transformations.map(contactEvents) {
            cenList -> cenList.fold(false) {
                isInfected: Boolean, cen: ContactEvent -> isInfected || cen.wasPotentiallyInfectious
            }
    }
    val hasPossiblyInteractedWithInfected: LiveData<Boolean> get() = _hasPossiblyInteractedWithInfected

    private val _isCurrentUserSick = MutableLiveData<Boolean>().apply {
        val isSick = application.getSharedPreferences(
            application.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).getBoolean(application.getString(R.string.preference_is_current_user_sick), false)
        value = isSick
    }
    val isCurrentUserSick: LiveData<Boolean> get() = _isCurrentUserSick

}