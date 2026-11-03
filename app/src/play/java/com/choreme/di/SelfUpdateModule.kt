package com.choreme.di

import com.choreme.core.update.SelfUpdate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The Google Play build is updated by Google Play: no checks, no banner, no installing apps. */
@Module
@InstallIn(SingletonComponent::class)
object SelfUpdateModule {
    @Provides
    fun selfUpdate() = SelfUpdate(enabled = false)
}
