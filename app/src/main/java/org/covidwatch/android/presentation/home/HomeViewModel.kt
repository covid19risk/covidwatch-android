package org.covidwatch.android.presentation.home

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.*
import org.covidwatch.android.R
import org.covidwatch.android.data.ContactEvent
import org.covidwatch.android.data.ContactEventDAO
import org.covidwatch.android.data.contactevent.ContactEventsDownloader
import org.covidwatch.android.domain.*
import org.covidwatch.android.presentation.util.Event
import org.covidwatch.android.presentation.util.getDistinct

class HomeViewModel(
    private val userFlowRepository: UserFlowRepository,
    private val testedRepository: TestedRepository,
    private val contactEventsDownloader: ContactEventsDownloader,
    contactEventDAO: ContactEventDAO
) : ViewModel() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    private val isUserTestedPositive: Boolean get() = testedRepository.isUserTestedPositive()
    private val _userTestedPositive = MutableLiveData<Unit>()
    val userTestedPositive: LiveData<Unit> get() = _userTestedPositive

    private val _locationPermissionAction = MutableLiveData<Event<Unit>>()
    val locationPermissionAction: LiveData<Event<Unit>> = _locationPermissionAction

    private val _turnOnBluetoothAction = MutableLiveData<Event<Unit>>()
    val turnOnBluetoothAction: LiveData<Event<Unit>> = _turnOnBluetoothAction

    private val _banner = MutableLiveData<Banner>()
    val banner: LiveData<Banner> get() = _banner

    private val _userFlow = MutableLiveData<UserFlow>()
    val userFlow: LiveData<UserFlow> get() = _userFlow

    private val _potentialRiskAction = MutableLiveData<Event<Unit>>()
    val potentialRiskAction: LiveData<Event<Unit>> get() = _potentialRiskAction

    private val _isRefreshing = MediatorLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> get() = _isRefreshing

    private val hasPossiblyInteractedWithInfected: LiveData<Boolean> =
        Transformations
            .map(contactEventDAO.allSortedByDescTimestamp) { cenList ->
                cenList.fold(initial = false) { isInfected: Boolean, event: ContactEvent ->
                    isInfected || event.wasPotentiallyInfectious
                }
            }
            .getDistinct()

    private val interactedWithInfectedObserver = Observer<Boolean> { hasPossiblyInteractedWithInfected ->
        if (hasPossiblyInteractedWithInfected && !isUserTestedPositive) {
            _banner.value = Banner.Warning(R.string.contact_alert_text, BannerAction.PotentialRisk)
        } else if (isUserTestedPositive) {
            _banner.value = Banner.Warning(R.string.reported_alert_text, BannerAction.PotentialRisk)
        }
    }

    init {
        hasPossiblyInteractedWithInfected.observeForever(interactedWithInfectedObserver)
        val userFlow = userFlowRepository.getUserFlow()
        if (userFlow !is Setup) {
            _locationPermissionAction.value = Event(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        hasPossiblyInteractedWithInfected.removeObserver(interactedWithInfectedObserver)
    }

    fun setup() {
        val userFlow = userFlowRepository.getUserFlow()
        if (userFlow is FirstTimeUser) {
            userFlowRepository.updateFirstTimeUserFlow()
        }
        if (userFlow !is Setup) {
            ensureBluetoothIsOn()
            checkIfTestedPositive()
        }
        _userFlow.value = userFlow
    }

    fun onBannerClicked() {
        val bannerAction = _banner.value?.action ?: return

        when (bannerAction) {
            is BannerAction.TurnOnBluetooth -> {
                _turnOnBluetoothAction.value = Event(Unit)
            }
            is BannerAction.PotentialRisk -> {
                _potentialRiskAction.value = Event(Unit)
            }
        }
    }

    fun onRefreshRequested() {
        val state = contactEventsDownloader.executePublicContactEventsRefresh()
        _isRefreshing.addSource(state) {
            _isRefreshing.value = !it
            if (_isRefreshing.value == false) {
                _isRefreshing.removeSource(state)
            }
        }
    }

    fun bluetoothIsOn() {
        _banner.value = Banner.Empty
    }

    private fun ensureBluetoothIsOn() {
        if (bluetoothAdapter?.isEnabled == false) {
            _banner.value = Banner.Info(R.string.turn_bluetooth_on, BannerAction.TurnOnBluetooth)
        }
    }

    private fun checkIfTestedPositive() {
        if (isUserTestedPositive) {
            _userTestedPositive.value = Unit
        }
    }
}