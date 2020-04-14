package org.covidwatch.android.ui.shared

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.covidwatch.android.data.IsCurrentSickSharedPreferenceLiveData

class SharedViewModelFactory(
    private val isCurrentSickSharedPreferenceLiveData: IsCurrentSickSharedPreferenceLiveData,
    private val application: Application
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SharedViewModel(isCurrentSickSharedPreferenceLiveData, application) as T
    }
}
