package com.choreme

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.choreme.ui.root.RootScreen
import com.choreme.core.ui.theme.ChoreMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
/** An AppCompat activity only so the language picked under This phone applies straight away. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChoreMeTheme {
                RootScreen()
            }
        }
    }
}
