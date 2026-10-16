package com.appanuman

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.appanuman.ui.MainViewModel
import com.appanuman.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Window security: Prevent screenshots and screen recording of audit results
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val targetPackage = intent?.getStringExtra("EXTRA_PACKAGE_NAME")

        setContent {
            val darkColors = darkColorScheme(
                primary = Color(0xFF1E88E5),
                background = Color(0xFF1A1A1A),
                surface = Color(0xFF242424),
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = darkColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1A1A1A)
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        initialPackageName = targetPackage
                    )
                }
            }
        }
    }
}
