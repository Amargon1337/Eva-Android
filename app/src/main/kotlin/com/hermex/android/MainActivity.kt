package com.hermex.android
import androidx.compose.foundation.layout.Box
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hermex.android.navigation.HermexNavGraph
import com.hermex.android.navigation.HermexIntentDestination
import com.hermex.android.navigation.hermexDestination
import com.hermex.android.ui.theme.HermexTheme
import androidx.lifecycle.lifecycleScope
import com.hermex.android.auth.serverUrlOrNull
import com.hermex.android.eva.EvaConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val externalIntentDestination = MutableStateFlow<HermexIntentDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalIntentDestination.value = intent?.hermexDestination()
        enableEdgeToEdge()
        val appContainer = (application as HermexApplication).appContainer

        lifecycleScope.launch {
            appContainer.authRepository.state.collect { state ->
                val url = state.serverUrlOrNull
                if (!url.isNullOrBlank()) {
                    EvaConfig.syncWithServerUrl(url)
                }
            }
        }
        setContent {
            HermexTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                  Box(Modifier.fillMaxSize()) {
                    com.hermex.android.ui.components.EvaModeParticles(Modifier.fillMaxSize())
                    HermexNavGraph(
                        appContainer = appContainer,
                        externalIntentDestination = externalIntentDestination,
                        onExternalIntentConsumed = { externalIntentDestination.value = null },
                    )
                  }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as HermexApplication).appContainer.setAppInForeground(true)
    }

    override fun onStop() {
        super.onStop()
        (application as HermexApplication).appContainer.setAppInForeground(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalIntentDestination.value = intent.hermexDestination()
    }
}
