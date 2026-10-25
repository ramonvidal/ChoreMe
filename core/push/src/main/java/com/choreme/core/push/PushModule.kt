package com.choreme.core.push

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PushModule {
    /** Lets the set of listeners be empty when no feature contributes one. */
    @Multibinds
    abstract fun listeners(): Set<PushListener>
}
