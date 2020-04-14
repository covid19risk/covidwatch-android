package org.covidwatch.android.ui.shared

import androidx.databinding.DataBindingComponent
import androidx.fragment.app.Fragment

/**
 * A Data Binding Component implementation for fragments.
 */
class FragmentDataBindingComponent(fragment: Fragment) : DataBindingComponent {

    private val adapter = FragmentBindingAdapters(fragment)

    fun getFragmentBindingAdapters() = adapter
}
