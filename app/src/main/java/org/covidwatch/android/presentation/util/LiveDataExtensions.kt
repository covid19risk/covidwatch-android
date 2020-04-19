package org.covidwatch.android.presentation.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

/**
 * Sets the value to the result of a function that is called when both `LiveData`s have data
 * or when they receive updates after that.
 */
fun <T, A, B> LiveData<A>.combineAndCompute(
    other: LiveData<B>,
    onChange: (A, B) -> T
): MediatorLiveData<T> {

    var firstSourceEmitted = false
    var secondSourceEmitted = false

    val result = MediatorLiveData<T>()

    val mergeF = {
        val firstSourceValue = this.value
        val secondSourceValue = other.value

        if (firstSourceEmitted && secondSourceEmitted) {
            result.value = onChange.invoke(firstSourceValue!!, secondSourceValue!!)
        }
    }

    result.addSource(this) {
        firstSourceEmitted = true
        mergeF.invoke()
    }
    result.addSource(other) {
        secondSourceEmitted = true
        mergeF.invoke()
    }

    return result
}

/**
 * Use this to avoid false positive notifications for observable Room queries
 * Source: [https://medium.com/androiddevelopers/7-pro-tips-for-room-fbadea4bfbd1]
 */
fun <T> LiveData<T>.getDistinct(): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()
    distinctLiveData.addSource(this, object : Observer<T> {
        private var initialized = false
        private var lastObj: T? = null
        override fun onChanged(obj: T?) {
            if (!initialized) {
                initialized = true
                lastObj = obj
                distinctLiveData.postValue(lastObj)
            } else if ((obj == null && lastObj != null)
                || obj != lastObj) {
                lastObj = obj
                distinctLiveData.postValue(lastObj)
            }
        }
    })
    return distinctLiveData
}