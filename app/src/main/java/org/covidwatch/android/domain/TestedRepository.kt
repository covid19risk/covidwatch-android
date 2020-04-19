package org.covidwatch.android.domain

interface TestedRepository {

    fun setUserTestedPositive()

    fun isUserTestedPositive(): Boolean
}