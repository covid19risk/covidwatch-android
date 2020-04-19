package org.covidwatch.android.data

import android.content.SharedPreferences
import org.covidwatch.android.domain.TestedRepository

private const val IS_CURRENT_USER_SICK_KEY = "preference_is_current_user_sick"

class TestedRepositoryImpl(
    private val preferences: SharedPreferences
) : TestedRepository {

    override fun setUserTestedPositive() {
        preferences.edit()
            .putBoolean(IS_CURRENT_USER_SICK_KEY, true)
            .apply()
    }

    override fun isUserTestedPositive(): Boolean {
        return preferences.getBoolean(IS_CURRENT_USER_SICK_KEY, false)
    }
}