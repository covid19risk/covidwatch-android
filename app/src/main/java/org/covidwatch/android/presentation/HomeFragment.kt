package org.covidwatch.android.presentation

import android.Manifest
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import org.covidwatch.android.BuildConfig
import org.covidwatch.android.R
import org.covidwatch.android.TcnManager
import org.covidwatch.android.databinding.FragmentHomeBinding
import org.covidwatch.android.domain.*
import org.covidwatch.android.presentation.home.Banner
import org.covidwatch.android.presentation.home.HomeViewModel
import org.covidwatch.android.presentation.util.EventObserver
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

private const val LOCATION_PERMISSION = 100
private const val REQUEST_ENABLE_BT = 101

class HomeFragment : Fragment(), EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModel()
    private val tcnManager: TcnManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel.locationPermissionAction.observe(this, EventObserver {
            ensureLocationPermissionIsGranted()
        })
    }

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

        binding.swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.colorPrimary))

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
        homeViewModel.potentialRiskAction.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(R.id.action_homeFragment_to_potentialRiskFragment)
        })
        homeViewModel.userTestedPositive.observe(viewLifecycleOwner, Observer {
            updateUiForTestedPositive()
        })
        homeViewModel.isRefreshing.observe(viewLifecycleOwner, Observer {
            updateRefreshingState(it)
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
        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.onRefreshRequested()
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
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_text)))
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

    private fun updateRefreshingState(isRefreshing: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = isRefreshing
    }

    @AfterPermissionGranted(LOCATION_PERMISSION)
    private fun ensureLocationPermissionIsGranted() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(requireContext(), *perms)) {
            tcnManager.start()
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.bluetooth_explanation_subtext),
                LOCATION_PERMISSION, *perms
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
        tcnManager.stop()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        tcnManager.start()
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