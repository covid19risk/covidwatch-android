package org.covidwatch.android.ui.userprofile

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.covidwatch.android.R
import org.covidwatch.android.databinding.FragmentUserProfileBinding
import org.covidwatch.android.ui.shared.FragmentDataBindingComponent


class UserProfileFragment : Fragment() {

    private val userProfileViewModel: UserProfileViewModel by viewModels()
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)
    private lateinit var binding: FragmentUserProfileBinding

    @SuppressLint("CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val context = context ?: return null

        binding =
            DataBindingUtil.inflate<FragmentUserProfileBinding>(
                inflater,
                R.layout.fragment_user_profile,
                container,
                false,
                dataBindingComponent
            ).apply {

                viewModel = userProfileViewModel
                lifecycleOwner = this@UserProfileFragment

                fun setCurrentUserSick(sick: Boolean) {
                    userProfileViewModel.isCurrentUserSick.value = sick
                    val application = context.applicationContext ?: return
                    val sharedPref = application.getSharedPreferences(
                        application.getString(R.string.preference_file_key), Context.MODE_PRIVATE
                    ) ?: return
                    with(sharedPref.edit()) {
                        putBoolean(
                            application.getString(R.string.preference_is_current_user_sick), sick
                        )
                        commit()
                    }
                }

                noButton.setOnClickListener {
                    setCurrentUserSick(false)
                }

                yesButton.setOnClickListener {
                    setCurrentUserSick(true)
                }
            }

        return binding.root
    }
}
