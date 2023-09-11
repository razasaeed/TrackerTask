package com.tracking.task

import android.app.Application
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.internal.jni.CoreArcGISRuntimeEnvironment
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HiltApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setApiKey()
    }

    private fun setApiKey() {
        CoreArcGISRuntimeEnvironment.setAPIKey(BuildConfig.GIS_API_KEY)
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.GIS_API_KEY)
    }
}