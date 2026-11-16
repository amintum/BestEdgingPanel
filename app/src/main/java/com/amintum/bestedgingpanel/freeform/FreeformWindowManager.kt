package com.amintum.bestedgingpanel.freeform

import android.content.Context
import android.util.Log

object FreeformWindowManager {
    private const val TAG = "FreeformWindowManager"

    var topWindow: String? = null
    private val activeWindows = mutableMapOf<String, FreeformWindow>()

    fun launchApp(context: Context, packageName: String): FreeformWindow? {
        Log.d(TAG, "Launching freeform app: $packageName")

        // If window already open, bring to focus or return existing
        activeWindows[packageName]?.let { existing ->
            existing.checkWindowOnTop()
            return existing
        }

        val config = FreeformConfig(packageName = packageName)
        val window = FreeformWindow(context.applicationContext, config) { closedWindow ->
            activeWindows.remove(closedWindow.getFreeformId())
            if (topWindow == closedWindow.getFreeformId()) {
                topWindow = activeWindows.keys.lastOrNull()
            }
            Log.d(TAG, "Closed freeform window for $packageName, remaining: ${activeWindows.size}")
        }

        activeWindows[packageName] = window
        topWindow = packageName
        return window
    }

    fun isAppOpen(packageName: String): Boolean {
        return activeWindows.containsKey(packageName)
    }

    fun removeWindow(id: String) {
        activeWindows.remove(id)
        if (topWindow == id) {
            topWindow = activeWindows.keys.lastOrNull()
        }
    }

    fun closeWindow(packageName: String) {
        activeWindows[packageName]?.destroy("closeWindow")
        activeWindows.remove(packageName)
    }

    fun closeAll() {
        val windows = activeWindows.values.toList()
        for (w in windows) {
            w.destroy("closeAll")
        }
        activeWindows.clear()
        topWindow = null
    }
}
