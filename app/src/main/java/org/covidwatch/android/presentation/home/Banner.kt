package org.covidwatch.android.presentation.home

sealed class Banner(val action: BannerAction) {

    data class Warning(
        val message: Int,
        val bannerAction: BannerAction
    ) : Banner(bannerAction)

    data class Info(
        val message: Int,
        val bannerAction: BannerAction
    ): Banner(bannerAction)

    object Empty : Banner(BannerAction.NoAction)
}

sealed class BannerAction {
    object TurnOnBluetooth : BannerAction()
    object PotentialRisk : BannerAction()
    object NoAction : BannerAction()
}