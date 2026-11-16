package com.amintum.bestedgingpanel.freeform

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.Log
import android.view.GestureDetector
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.math.max
import kotlin.math.roundToInt

class MoveTouchListener(
    private val window: FreeformWindow
) : View.OnTouchListener {
    private var startX = 0.0f
    private var startY = 0.0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                try {
                    window.windowManager.updateViewLayout(window.freeformLayout, window.windowParams.apply {
                        x = (x + event.rawX - startX).roundToInt()
                        y = (y + event.rawY - startY).roundToInt()
                    })
                } catch (_: Exception) {}
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                window.makeSureFreeformInScreen()
                window.checkWindowOnTop()
            }
        }
        return true
    }
}

class ScaleTouchListener(
    private val window: FreeformWindow,
    private val isRight: Boolean = true
) : View.OnTouchListener {
    private var startX = 0.0f
    private var startY = 0.0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val minWidth = 360
                val minHeight = 480
                window.freeformRootView.layoutParams = window.freeformRootView.layoutParams.apply {
                    width = max(minWidth, (window.freeformRootView.width + if (isRight) (event.rawX - startX) else (startX - event.rawX)).roundToInt())
                    height = max(minHeight, (window.freeformRootView.height + event.rawY - startY).roundToInt())
                }
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                if (window.freeformView.surfaceTexture != null) {
                    window.freeformConfig.width = window.freeformRootView.layoutParams.width
                    window.freeformConfig.height = window.freeformRootView.layoutParams.height
                    window.handler.post { window.makeSureFreeformInScreen() }
                    window.measureScale()
                    window.resizeVirtualDisplay(
                        window.freeformConfig.freeformWidth,
                        window.freeformConfig.freeformHeight,
                        window.freeformConfig.densityDpi
                    )
                    window.freeformView.surfaceTexture?.setDefaultBufferSize(
                        window.freeformConfig.freeformWidth,
                        window.freeformConfig.freeformHeight
                    )
                }
                window.checkWindowOnTop()
            }
        }
        return true
    }
}

class HangUpGestureListener(private val window: FreeformWindow) : GestureDetector.SimpleOnGestureListener() {
    private var startX = 0
    private var startY = 0

    override fun onDown(e: MotionEvent): Boolean {
        startX = window.windowParams.x
        startY = window.windowParams.y
        return super.onDown(e)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        window.handler.post { window.handleHangUp() }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (null == e1) return true
        window.handler.post {
            try {
                window.windowManager.updateViewLayout(window.freeformLayout, window.windowParams.apply {
                    x = (startX + e2.rawX - e1.rawX).roundToInt()
                    y = (startY + e2.rawY - e1.rawY).roundToInt()
                })
            } catch (_: Exception) {}
        }
        return true
    }
}

class FreeformInputTouchListener(
    private val context: Context,
    private val window: FreeformWindow
) : View.OnTouchListener {
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager

    companion object {
        private const val TAG = "FreeformTouchListener"

        fun injectInputEvent(inputManager: InputManager, event: InputEvent, displayId: Int): Boolean {
            // Set displayId directly using HiddenApiBypass
            try {
                HiddenApiBypass.invoke(InputEvent::class.java, event, "setDisplayId", displayId)
            } catch (e: Exception) {
                try {
                    val setDisplayIdMethod = InputEvent::class.java.getDeclaredMethod("setDisplayId", Int::class.javaPrimitiveType)
                    setDisplayIdMethod.isAccessible = true
                    setDisplayIdMethod.invoke(event, displayId)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to set displayId $displayId on event", e2)
                }
            }

            // Inject the event via InputManager
            try {
                val injectMethod = InputManager::class.java.getMethod(
                    "injectInputEvent",
                    InputEvent::class.java,
                    Int::class.javaPrimitiveType
                )
                val success = injectMethod.invoke(inputManager, event, 0) as? Boolean ?: true
                return success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to inject event into display $displayId", e)
                return false
            }
        }

        fun injectBackKey(context: Context, displayId: Int) {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager ?: return
            val now = SystemClock.uptimeMillis()
            val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0).apply {
                source = InputDevice.SOURCE_KEYBOARD
            }
            val up = KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0).apply {
                source = InputDevice.SOURCE_KEYBOARD
            }
            injectInputEvent(inputManager, down, displayId)
            injectInputEvent(inputManager, up, displayId)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val displayId = window.freeformConfig.displayId
        if (displayId < 0 || inputManager == null) return false

        if (event.action == MotionEvent.ACTION_DOWN) {
            view.requestFocus()
        }

        val pointerCoords = Array(event.pointerCount) { i ->
            MotionEvent.PointerCoords().apply {
                event.getPointerCoords(i, this)
                x *= window.freeformConfig.scale
                y *= window.freeformConfig.scale
            }
        }

        val pointerProperties = Array(event.pointerCount) { i ->
            MotionEvent.PointerProperties().apply {
                event.getPointerProperties(i, this)
            }
        }

        val newEvent = MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            event.action,
            event.pointerCount,
            pointerProperties,
            pointerCoords,
            event.metaState,
            event.buttonState,
            event.xPrecision,
            event.yPrecision,
            event.deviceId,
            event.edgeFlags,
            InputDevice.SOURCE_TOUCHSCREEN,
            event.flags
        )
        newEvent.source = InputDevice.SOURCE_TOUCHSCREEN

        injectInputEvent(inputManager, newEvent, displayId)
        newEvent.recycle()

        if (event.action == MotionEvent.ACTION_UP) {
            window.checkWindowOnTop()
        }
        return true
    }
}
