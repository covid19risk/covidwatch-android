package org.covidwatch.android.data

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

class IsCurrentSickSharedPreferenceLiveData(private val sharedPreferences: SharedPreferences) : LiveData<Boolean>() {

    private val isCurrentSickPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
            if (key == IS_CURRENT_SICK) {
                value = sharedPreferences?.getBoolean(IS_CURRENT_SICK, false)
            }
        }


    override fun onActive() {
        super.onActive()
        value = sharedPreferences.getBoolean(IS_CURRENT_SICK, false)
        sharedPreferences.registerOnSharedPreferenceChangeListener(isCurrentSickPreferenceListener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(isCurrentSickPreferenceListener)
    }

    companion object {
        private const val IS_CURRENT_SICK = "preference_is_current_user_sick"

    }
}