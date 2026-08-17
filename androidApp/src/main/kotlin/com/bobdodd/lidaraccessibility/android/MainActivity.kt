package com.bobdodd.lidaraccessibility.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bobdodd.lidaraccessibility.android.di.AppComponent
import com.bobdodd.lidaraccessibility.android.ui.diagnostics.DiagnosticsScreen
import com.bobdodd.lidaraccessibility.android.ui.disclaimer.DisclaimerGate
import com.bobdodd.lidaraccessibility.android.ui.knowledge.KnowledgeMapScreen
import com.bobdodd.lidaraccessibility.android.ui.theme.LidarAccessibilityTheme

class MainActivity : ComponentActivity() {

    private lateinit var component: AppComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = AppComponent(applicationContext)
        enableEdgeToEdge()

        setContent {
            LidarAccessibilityTheme {
                // 0 = disclaimer, 1 = diagnostics, 2 = knowledge map
                var screen by remember { mutableIntStateOf(0) }

                when (screen) {
                    0 -> DisclaimerGate(onAccept = { screen = 1 })
                    1 -> DiagnosticsScreen(
                        component = component,
                        context = this,
                        onContinue = { screen = 2 },
                    )
                    2 -> KnowledgeMapScreen(chat = component.chat)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::component.isInitialized) {
            component.shutdown()
        }
    }
}
