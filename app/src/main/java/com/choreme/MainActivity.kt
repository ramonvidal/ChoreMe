package com.choreme

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.choreme.core.settings.AppearanceSettings
import com.choreme.core.settings.ThemeMode
import com.choreme.core.ui.theme.ChoreMeTheme
import com.choreme.ui.root.RootScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
/** An AppCompat activity only so the language picked under This phone applies straight away. */
class MainActivity : AppCompatActivity() {
    @Inject lateinit var appearance: AppearanceSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by appearance.mode.collectAsState()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // The status and navigation bar icons follow the look chosen here, not only the phone's.
            DisposableEffect(dark) {
                val bars = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose { }
            }
            ChoreMeTheme(darkTheme = dark) {
                RootScreen()
            }
        }
    }
}
