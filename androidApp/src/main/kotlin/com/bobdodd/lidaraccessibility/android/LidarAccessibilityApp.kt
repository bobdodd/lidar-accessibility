package com.bobdodd.lidaraccessibility.android

import android.app.Application
import com.bobdodd.lidaraccessibility.android.di.AppComponent

class LidarAccessibilityApp : Application() {

    lateinit var component: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        component = AppComponent(applicationContext)
    }
}
