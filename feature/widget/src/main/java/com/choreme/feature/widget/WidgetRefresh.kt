package com.choreme.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.startup.Initializer
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the widget current without polling: it refreshes when the app goes to the
 * background (whatever was just changed shows up on the home screen) and when another
 * phone pings about a chore. Android also refreshes it every 30 minutes.
 */
class WidgetRefreshInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val app = context.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                owner.lifecycleScope.launch { ChoreWidget().updateAll(app) }
            }
        })
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(ProcessLifecycleInitializer::class.java)
}

internal class WidgetPushListener @Inject constructor(@ApplicationContext private val context: Context) : PushListener {
    override fun onPing(kind: String) {
        if (kind in REFRESH_ON) scope.launch { ChoreWidget().updateAll(context) }
    }

    private companion object {
        val REFRESH_ON = setOf(PushKinds.CHORE_ADDED, PushKinds.CHORE_TO_APPROVE)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetPushModule {
    @Binds
    @IntoSet
    abstract fun widget(listener: WidgetPushListener): PushListener
}
