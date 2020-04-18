package org.covidwatch.android.domain

interface TestRepository {

    fun setUserTestedPositive()

    fun isUserTestedPositive(): Boolean
}