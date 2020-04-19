package org.covidwatch.android.presentation.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.covidwatch.android.R
import org.covidwatch.android.databinding.FragmentTestConfirmationBinding
import org.covidwatch.android.domain.TestedRepository
import org.koin.android.ext.android.inject

class TestConfirmationFragment : Fragment() {

    private var _binding: FragmentTestConfirmationBinding? = null
    private val binding get() = _binding!!

    private val testedRepository: TestedRepository by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTestConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.confirmButton.setOnClickListener {
            showConfirmationDialog()
        }
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.closeButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure?")
            .setPositiveButton("Yes") { dialogInterface, i ->
                testedRepository.setUserTestedPositive()
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}