package com.appanuman

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appanuman.ui.navigation.AppNavGraph
import com.appanuman.ui.theme.AppAnumanTheme
import com.appanuman.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Window Security: Prevents screenshots and screen recording of sensitive scan results
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Error applying FLAG_SECURE", t)
        }

        setContent {
            val userMode by viewModel.userMode.collectAsStateWithLifecycle()

            AppAnumanTheme(userMode = userMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
