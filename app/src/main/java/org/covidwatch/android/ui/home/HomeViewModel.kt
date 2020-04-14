package org.covidwatch.android.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.covidwatch.android.R

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _isCurrentUserSick = MutableLiveData<Boolean>().apply {
        val isSick = application.getSharedPreferences(
            application.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).getBoolean(application.getString(R.string.preference_is_current_user_sick), false)
        value = isSick
    }
    val isCurrentUserSick: LiveData<Boolean> get() = _isCurrentUserSick

    private val _initialVisit = MutableLiveData<Boolean>().apply {
        value = application.getSharedPreferences(
            application.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        ).getBoolean(application.getString(R.string.preference_initial_visit), true)
    }
    val initialVisit: LiveData<Boolean> get() = _initialVisit

}