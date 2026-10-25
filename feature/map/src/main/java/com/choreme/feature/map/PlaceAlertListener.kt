package com.choreme.feature.map

import android.content.Context
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/** Another phone says someone arrived or left: check now instead of waiting. */
internal class PlaceAlertListener @Inject constructor(@ApplicationContext private val context: Context) : PushListener {
    override fun onPing(kind: String) {
        if (kind == PushKinds.PLACE_EVENTS) PlaceAlerts.checkNow(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaceAlertListenerModule {
    @Binds
    @IntoSet
    abstract fun placeAlerts(listener: PlaceAlertListener): PushListener
}
