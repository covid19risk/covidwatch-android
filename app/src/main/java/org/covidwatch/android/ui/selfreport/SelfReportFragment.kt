package org.covidwatch.android

import android.app.DatePickerDialog
import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import org.covidwatch.android.databinding.FragmentSelfReportBinding
import org.covidwatch.android.ui.selfreport.SelfReportViewModel
import org.covidwatch.android.ui.shared.FragmentDataBindingComponent
import java.text.SimpleDateFormat
import java.util.*


class SelfReportFragment : Fragment() {

    private val selfReportProfileViewModel: SelfReportViewModel by viewModels()
    private var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)
    private lateinit var binding: FragmentSelfReportBinding
    private val symptomsStartDateCalendar: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val context = context ?: return null

        binding =
            DataBindingUtil.inflate<FragmentSelfReportBinding>(
                inflater,
                R.layout.fragment_self_report,
                container,
                false,
                dataBindingComponent
            ).apply {

                viewModel = selfReportProfileViewModel
                lifecycleOwner = this@SelfReportFragment

                fun setCurrentUserSick(sick: Boolean) {
                    selfReportProfileViewModel.isCurrentUserSick.value = sick
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

                datePicker.setOnClickListener {
                    DatePickerDialog(context,
                        getSymptomsStartDateListener(),
                        symptomsStartDateCalendar.get(Calendar.YEAR),
                        symptomsStartDateCalendar.get(Calendar.MONTH),
                        symptomsStartDateCalendar.get(Calendar.DAY_OF_MONTH))
                        .show()
                }

                report.setOnClickListener {
                    val confirmBuilder: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(
                        ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom)
                    )
                    // customize style of confirmation alert
                    confirmBuilder.setTitle("Correct Information?")
                    confirmBuilder.setMessage("I attest that the information I've submitted is true to the best of my knowledge")

                    confirmBuilder.setPositiveButton(
                        "CONFIRM"
                    ) { dialog, which ->
                        setCurrentUserSick(true)
                        Toast.makeText(
                            getActivity(),
                            "Your report was submitted.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    confirmBuilder.setNegativeButton(
                        "CANCEL"
                    ) { dialog, which ->
                        Toast.makeText(
                            getActivity(),
                            "Pressed Cancel",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    confirmBuilder.show()
                }

            }

        updateSymptomsStartDateLabel()

        return binding.root
    }

    private fun getSymptomsStartDateListener() : DatePickerDialog.OnDateSetListener {
        return DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            symptomsStartDateCalendar.set(year, month, dayOfMonth)
            updateSymptomsStartDateLabel()
        }
    }

    private fun updateSymptomsStartDateLabel() {
        val dateFormat = SimpleDateFormat.getDateInstance()
        binding.datePicker.setText(dateFormat.format(symptomsStartDateCalendar.time))
    }
}
