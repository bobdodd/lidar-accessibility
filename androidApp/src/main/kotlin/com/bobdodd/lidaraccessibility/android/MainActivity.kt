package com.bobdodd.lidaraccessibility.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bobdodd.lidaraccessibility.android.ui.disclaimer.DisclaimerGate
import com.bobdodd.lidaraccessibility.android.ui.knowledge.KnowledgeMapScreen
import com.bobdodd.lidaraccessibility.android.ui.theme.LidarAccessibilityTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LidarAccessibilityTheme {
                var accepted by remember { mutableStateOf(false) }
                if (!accepted) {
                    DisclaimerGate(onAccept = { accepted = true })
                } else {
                    KnowledgeMapScreen()
                }
            }
        }
    }
}
