package com.bobdodd.lidaraccessibility.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.bobdodd.lidaraccessibility.android.di.AppComponent
import com.bobdodd.lidaraccessibility.android.ui.depth.DepthDiagnosticsScreen
import com.bobdodd.lidaraccessibility.android.ui.diagnostics.DiagnosticsScreen
import com.bobdodd.lidaraccessibility.android.ui.disclaimer.DisclaimerGate
import com.bobdodd.lidaraccessibility.android.ui.knowledge.KnowledgeMapScreen
import com.bobdodd.lidaraccessibility.android.ui.theme.LidarAccessibilityTheme

class MainActivity : ComponentActivity() {

    private lateinit var component: AppComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component = AppComponent(this)
        enableEdgeToEdge()

        setContent {
            LidarAccessibilityTheme {
                // 0 = disclaimer, 1 = platform diagnostics, 2 = depth diagnostics, 3 = knowledge map
                var screen by remember { mutableIntStateOf(0) }
                var cameraGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    cameraGranted = granted
                    if (granted) {
                        component.startDepth()
                    }
                }

                when (screen) {
                    0 -> DisclaimerGate(onAccept = { screen = 1 })
                    1 -> DiagnosticsScreen(
                        component = component,
                        context = this,
                        onContinue = {
                            component.startSensors()
                            screen = 2
                        },
                    )
                    2 -> DepthDiagnosticsScreen(
                        depthState = component.depthProvider.state,
                        depthFrame = component.depthProvider.frame,
                        cameraPermissionGranted = cameraGranted,
                        onRequestCameraPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onStart = {
                            if (cameraGranted) {
                                component.startDepth()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onStop = { component.stopDepth() },
                        onContinue = {
                            component.stopDepth()
                            screen = 3
                        },
                    )
                    3 -> KnowledgeMapScreen(
                        chat = component.chat,
                        heading = component.heading.heading,
                        followMeEvent = component.followMe.lastEvent,
                        isFollowing = component.followMe.isFollowing,
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::component.isInitialized) {
            component.stopDepth()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::component.isInitialized) {
            component.shutdown()
        }
    }
}
