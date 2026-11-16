package com.amintum.bestedgingpanel.freeform

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class FreeformConfig(
    val packageName: String,
    var width: Int = 540,
    var height: Int = 1200,
    var densityDpi: Int = 320,
    var freeformWidth: Int = 1080,
    var freeformHeight: Int = 2400,
    var scale: Float = 2.0f,
    var isHangUp: Boolean = false,
    var hangUpWidth: Int = 300,
    var hangUpHeight: Int = 400,
    var notInHangUpX: Int = 0,
    var notInHangUpY: Int = 0,
    var displayId: Int = -1,
    var taskId: Int = -1
) {
    fun measureScale(defaultDisplayWidth: Int, defaultDisplayHeight: Int) {
        val safeW = max(50, width)
        val safeH = max(50, height)
        val widthScale = min(defaultDisplayWidth, defaultDisplayHeight) * 1.0f / min(safeW, safeH)
        val heightScale = max(defaultDisplayWidth, defaultDisplayHeight) * 1.0f / max(safeW, safeH)
        scale = min(widthScale, heightScale)
        freeformWidth = (safeW * scale).roundToInt()
        freeformHeight = (safeH * scale).roundToInt()
    }
}
