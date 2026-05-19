package com.Taybetibrowser.security

import android.app.Activity
import android.view.WindowManager

class AntiScreenshot(private val activity: Activity) {

    fun enable() {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun disable() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun initialize() {
        enable()
    }

    fun onAppBackgrounded() {}

    fun onAppExit() {}
}