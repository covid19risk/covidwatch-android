package org.covidwatch.android.presentation

import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import org.covidwatch.android.BuildConfig
import org.covidwatch.android.R
import org.covidwatch.android.databinding.FragmentHomeBinding
import org.covidwatch.android.domain.FirstTimeUser
import org.covidwatch.android.domain.ReturnUser
import org.covidwatch.android.domain.Setup
import org.covidwatch.android.domain.UserFlow
import org.covidwatch.android.presentation.home.Banner
import org.covidwatch.android.presentation.home.HomeViewModel
import org.covidwatch.android.presentation.util.EventObserver
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val REQUEST_ENABLE_BT = 101

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel.setup()
        homeViewModel.userFlow.observe(viewLifecycleOwner, Observer {
            handleUserFlow(it)
        })
        homeViewModel.banner.observe(viewLifecycleOwner, Observer {
            maybeShowBanner(it)
        })
        homeViewModel.turnOnBluetoothAction.observe(viewLifecycleOwner, EventObserver {
            turnOnBluetooth()
        })
        homeViewModel.userTestedPositive.observe(viewLifecycleOwner, Observer {
            updateUiForTestedPositive()
        })

        initClickListeners()
    }

    private fun initClickListeners() {
        binding.testedButton.setOnClickListener {
            findNavController().navigate(R.id.testQuestionsFragment)
        }
        binding.menuButton.setOnClickListener {
            findNavController().navigate(R.id.menuFragment)
        }
        binding.shareAppButton.setOnClickListener {
            shareApp()
        }
        binding.bannerText.setOnClickListener {
            homeViewModel.onBannerClicked()
        }
    }

    private fun shareApp() {
        val shareText = getString(R.string.share_intent_text)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(
            Intent.EXTRA_TEXT,
            "$shareText https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
        )
        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    }

    private fun handleUserFlow(userFlow: UserFlow) {
        when (userFlow) {
            is FirstTimeUser -> {
                updateUiForFirstTimeUser()
            }
            is Setup -> {
                findNavController().navigate(R.id.splashFragment)
            }
            is ReturnUser -> {
                updateUiForReturnUser()
            }
        }
    }

    private fun updateUiForFirstTimeUser() {
        binding.homeTitle.setText(R.string.you_re_all_set_title)
        binding.homeSubtitle.setText(R.string.thank_you_text)
    }

    private fun updateUiForReturnUser() {
        binding.homeTitle.setText(R.string.welcome_back_title)
        binding.homeSubtitle.setText(R.string.not_detected_text)
    }

    private fun updateUiForTestedPositive() {
        binding.homeSubtitle.setText(R.string.reported_tested_positive_text)
        binding.testedButton.isVisible = false
        binding.testedButtonText.isVisible = false
    }

    private fun maybeShowBanner(banner: Banner) {
        when (banner) {
            is Banner.Warning -> {
                binding.bannerText.isVisible = true
                binding.bannerText.setText(banner.message)
                binding.bannerText.setBackgroundResource(R.color.tangerine)
            }
            is Banner.Info -> {
                binding.bannerText.isVisible = true
                binding.bannerText.setText(banner.message)
                binding.bannerText.setBackgroundResource(R.color.aqua)
            }
            is Banner.Empty -> {
                binding.bannerText.isVisible = false
            }
        }
    }

    private fun turnOnBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            homeViewModel.bluetoothIsOn()
        }
    }
}