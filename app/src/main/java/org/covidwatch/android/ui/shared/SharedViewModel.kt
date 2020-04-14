package org.covidwatch.android.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import org.covidwatch.android.data.ContactEvent
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.data.IsCurrentSickSharedPreferenceLiveData

class SharedViewModel(
    private val isCurrentSickSharedPreferenceLiveData: IsCurrentSickSharedPreferenceLiveData,
    application: Application
) : AndroidViewModel(application) {

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

    private val _isCurrentUserSick = isCurrentSickSharedPreferenceLiveData
    val isCurrentUserSick: LiveData<Boolean> get() = _isCurrentUserSick

}