package org.covidwatch.android.di

import android.content.Context
import androidx.work.WorkManager
import org.covidwatch.android.data.CovidWatchDatabase
import org.covidwatch.android.data.TestRepositoryImpl
import org.covidwatch.android.domain.UserFlowRepository
import org.covidwatch.android.data.UserFlowRepositoryImpl
import org.covidwatch.android.domain.MaybeEnableContactEventLoggingUseCase
import org.covidwatch.android.domain.RefreshPublicContactEventsUseCase
import org.covidwatch.android.domain.TestRepository
import org.covidwatch.android.presentation.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    factory {
        UserFlowRepositoryImpl(
            preferences = get()
        ) as UserFlowRepository
    }

    factory {
        val context = androidContext()

        context.getSharedPreferences("org.covidwatch.android.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
    }

    viewModel {
        HomeViewModel(
            userFlowRepository = get(),
            testRepository = get(),
            refreshPublicContactEventsUseCase = get(),
            maybeEnableContactEventLoggingUseCase = get(),
            contactEventDAO = get()
        )
    }

    single {
        CovidWatchDatabase.getInstance(androidContext())
    }

    single {
        val database: CovidWatchDatabase = get()

        database.contactEventDAO()
    }

    factory {
        TestRepositoryImpl(
            preferences = get()
        ) as TestRepository
    }

    factory {
        val workManager = WorkManager.getInstance(androidContext())

        RefreshPublicContactEventsUseCase(workManager)
    }

    factory {
        MaybeEnableContactEventLoggingUseCase(
            context = androidContext(),
            preferences = get()
        )
    }
}