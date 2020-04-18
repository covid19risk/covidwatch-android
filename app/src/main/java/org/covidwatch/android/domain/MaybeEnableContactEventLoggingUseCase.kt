package org.covidwatch.android.domain

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import pub.devrel.easypermissions.EasyPermissions

private const val IS_CONTACT_EVENT_LOGGING_ENABLED_KEY = "preference_is_contact_event_logging_enabled"

class MaybeEnableContactEventLoggingUseCase(
    private val context: Context,
    private val preferences: SharedPreferences
) {

    fun execute() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(context, *perms)) {
            preferences
                .edit()
                .putBoolean(IS_CONTACT_EVENT_LOGGING_ENABLED_KEY, true)
                .apply()
        }
    }
}