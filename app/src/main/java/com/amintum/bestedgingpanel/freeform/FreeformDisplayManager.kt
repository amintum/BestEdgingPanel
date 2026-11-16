package com.amintum.bestedgingpanel.freeform

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.Log
import android.view.Surface

object FreeformDisplayManager {
    private const val TAG = "FreeformDisplayManager"

    private const val FLAG_TRUSTED_FALLBACK = 1 shl 10 // VIRTUAL_DISPLAY_FLAG_TRUSTED = 1024

    fun createDisplay(
        context: Context,
        name: String,
        width: Int,
        height: Int,
        densityDpi: Int,
        surface: Surface
    ): VirtualDisplay? {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        if (dm == null) {
            Log.e(TAG, "DisplayManager service not available")
            return null
        }

        val trustedFlag = try {
            DisplayManager::class.java.getField("VIRTUAL_DISPLAY_FLAG_TRUSTED").getInt(null)
        } catch (_: Exception) {
            FLAG_TRUSTED_FALLBACK
        }

        val primaryFlags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                trustedFlag

        val safeW = width.coerceAtLeast(100)
        val safeH = height.coerceAtLeast(100)
        val safeDpi = densityDpi.coerceIn(120, 640)

        return try {
            Log.d(TAG, "Creating trusted virtual display $name: ${safeW}x${safeH}@${safeDpi}dpi")
            dm.createVirtualDisplay(name, safeW, safeH, safeDpi, surface, primaryFlags)
        } catch (e: SecurityException) {
            Log.w(TAG, "Trusted flag not permitted, falling back to standard public display: ${e.message}")
            try {
                val fallbackFlags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                dm.createVirtualDisplay(name, safeW, safeH, safeDpi, surface, fallbackFlags)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to create fallback virtual display", e2)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating virtual display", e)
            null
        }
    }

    fun resizeDisplay(virtualDisplay: VirtualDisplay?, width: Int, height: Int, densityDpi: Int) {
        if (virtualDisplay == null) return
        try {
            val safeW = width.coerceAtLeast(100)
            val safeH = height.coerceAtLeast(100)
            val safeDpi = densityDpi.coerceIn(120, 640)
            virtualDisplay.resize(safeW, safeH, safeDpi)
            Log.d(TAG, "Virtual display resized: ${safeW}x${safeH}@${safeDpi}dpi")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resize virtual display", e)
        }
    }

    fun releaseDisplay(virtualDisplay: VirtualDisplay?) {
        if (virtualDisplay == null) return
        try {
            virtualDisplay.release()
            Log.d(TAG, "Virtual display released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release virtual display", e)
        }
    }
}
