package com.choreme.di

import com.choreme.core.update.SelfUpdate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The build on the project's release page checks for new versions and installs them itself. */
@Module
@InstallIn(SingletonComponent::class)
object SelfUpdateModule {
    @Provides
    fun selfUpdate() = SelfUpdate(enabled = true)
}
