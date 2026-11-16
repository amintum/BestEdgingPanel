package com.amintum.bestedgingpanel.freeform

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.amintum.bestedgingpanel.R
import com.amintum.bestedgingpanel.dpToPx
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FreeformWindow(
    val context: Context,
    val freeformConfig: FreeformConfig,
    private val onCloseCallback: (FreeformWindow) -> Unit
) : TextureView.SurfaceTextureListener {

    companion object {
        private const val TAG = "FreeformWindow"
    }

    val handler: Handler = Handler(Looper.getMainLooper())
    val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowParams = WindowManager.LayoutParams()

    lateinit var freeformLayout: ViewGroup
    lateinit var freeformRootView: FrameLayout
    lateinit var freeformView: TextureView
    lateinit var topBarView: View
    lateinit var bottomBarView: View

    private lateinit var appIconView: ImageView
    private lateinit var appTitleView: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnMaximize: ImageButton
    private lateinit var btnClose: ImageButton

    var defaultDisplayWidth = context.resources.displayMetrics.widthPixels
    var defaultDisplayHeight = context.resources.displayMetrics.heightPixels

    private var virtualDisplay: VirtualDisplay? = null
    private var surface: Surface? = null
    private var isDestroyed = false
    private var splashView: ImageView? = null
    private var hasRenderedFirstFrame = false
    private var subscreenDepth = 0

    private val hangUpGestureListener = HangUpGestureListener(this)

    init {
        // Matched to user's exact adjusted dimensions from live screen (user_manual_now.png):
        // Outer card: 724px width, 1406px height on 1080x2460 display (67.04% width, 57.15% height).
        // Total outer height = freeformRootView height + header (36dp) + bottom bar (14dp).
        val targetOuterWidth = (defaultDisplayWidth * 0.6704f).roundToInt()
        val targetOuterHeight = (defaultDisplayHeight * 0.5715f).roundToInt()
        val headerBottomBarsPx = context.dpToPx(36 + 14)

        val initW = targetOuterWidth.coerceIn(400, defaultDisplayWidth)
        val initH = (targetOuterHeight - headerBottomBarsPx).coerceIn(400, defaultDisplayHeight)
        freeformConfig.width = initW
        freeformConfig.height = initH
        freeformConfig.densityDpi = context.resources.displayMetrics.densityDpi
        measureScale()

        handler.post {
            if (!addFreeformView()) {
                destroy("init:addFreeformView failed")
            }
        }
    }

    fun measureScale() {
        freeformConfig.measureScale(defaultDisplayWidth, defaultDisplayHeight)
    }

    @SuppressLint("InflateParams")
    private fun addFreeformView(): Boolean {
        try {
            freeformLayout = LayoutInflater.from(context).inflate(R.layout.view_freeform, null) as ViewGroup
            freeformRootView = freeformLayout.findViewById(R.id.freeform_root)
            topBarView = freeformLayout.findViewById(R.id.topBarView)
            bottomBarView = freeformLayout.findViewById(R.id.bottomBar)

            appIconView = freeformLayout.findViewById(R.id.freeform_app_icon)
            appTitleView = freeformLayout.findViewById(R.id.freeform_app_title)
            btnBack = freeformLayout.findViewById(R.id.btn_back)
            btnMaximize = freeformLayout.findViewById(R.id.btn_maximize)
            btnClose = freeformLayout.findViewById(R.id.btn_close)

            val middleView = freeformLayout.findViewById<View>(R.id.middleView)
            val leftScaleView = freeformLayout.findViewById<View>(R.id.leftScale)
            val rightScaleView = freeformLayout.findViewById<View>(R.id.rightScale)

            // Populate App Icon and Title
            loadAppMetadata()

            // Header button click listeners
            btnBack.setOnClickListener {
                injectBackKey()
            }

            btnMaximize.setOnClickListener {
                maximizeApp()
            }

            btnClose.setOnClickListener {
                destroy("user_close_button")
            }

            // Window dragging via header and bottom middle drag bar
            val moveTouchListener = MoveTouchListener(this)
            topBarView.setOnTouchListener(moveTouchListener)
            appTitleView.setOnTouchListener(moveTouchListener)
            middleView.setOnTouchListener(moveTouchListener)

            // Dual corner resize handles (bottom-left and bottom-right)
            leftScaleView.setOnTouchListener(ScaleTouchListener(this, false))
            rightScaleView.setOnTouchListener(ScaleTouchListener(this, true))

            // VirtualDisplay TextureView inside freeformRootView
            freeformView = FreeformTextureView(context).apply {
                setOnTouchListener(FreeformInputTouchListener(context, this@FreeformWindow))
                surfaceTextureListener = this@FreeformWindow
            }

            freeformRootView.layoutParams = freeformRootView.layoutParams.apply {
                width = freeformConfig.width
                height = freeformConfig.height
            }
            freeformRootView.addView(freeformView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            // Centered splash placeholder icon to prevent any transparent flash during launch
            splashView = ImageView(context).apply {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(freeformConfig.packageName, 0)
                    setImageDrawable(context.packageManager.getApplicationIcon(appInfo))
                } catch (_: Exception) {}
                scaleType = ImageView.ScaleType.FIT_CENTER
                alpha = 0.35f
            }
            val splashLp = FrameLayout.LayoutParams(context.dpToPx(56), context.dpToPx(56), Gravity.CENTER)
            freeformRootView.addView(splashView, splashLp)

            freeformLayout.isFocusable = true
            freeformLayout.isFocusableInTouchMode = true
            freeformLayout.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    injectBackKey()
                    true
                } else {
                    false
                }
            }
            freeformView.isFocusable = true
            freeformView.isFocusableInTouchMode = true

            windowParams.apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                format = PixelFormat.RGBA_8888
                gravity = Gravity.CENTER
                x = 0
                y = 0
                windowAnimations = android.R.style.Animation_Dialog
            }

            windowManager.addView(freeformLayout, windowParams)
            FreeformWindowManager.topWindow = getFreeformId()
            Log.d(TAG, "addFreeformView succeeded: ${freeformConfig.width}x${freeformConfig.height} centered")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "addFreeformView failed", e)
            return false
        }
    }

    private fun loadAppMetadata() {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(freeformConfig.packageName, 0)
            appIconView.setImageDrawable(pm.getApplicationIcon(appInfo))
            val label = pm.getApplicationLabel(appInfo).toString()
            appTitleView.text = label
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load app metadata for ${freeformConfig.packageName}", e)
            appTitleView.text = freeformConfig.packageName
        }
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureAvailable: ${width}x$height, buffer: ${freeformConfig.freeformWidth}x${freeformConfig.freeformHeight}")
        surfaceTexture.setDefaultBufferSize(freeformConfig.freeformWidth, freeformConfig.freeformHeight)
        surface = Surface(surfaceTexture)

        val vd = FreeformDisplayManager.createDisplay(
            context,
            "BestFreeform_${freeformConfig.packageName}",
            freeformConfig.freeformWidth,
            freeformConfig.freeformHeight,
            freeformConfig.densityDpi,
            surface!!
        )

        if (vd == null || vd.display == null) {
            Log.e(TAG, "Failed to create virtual display")
            destroy("createDisplay failed")
            return
        }

        virtualDisplay = vd
        freeformConfig.displayId = vd.display.displayId
        Log.d(TAG, "Virtual display initialized: displayId=${freeformConfig.displayId}")

        // Force virtual display IME policy to FALLBACK_DISPLAY (1) so soft keyboard appears on default display
        setVirtualDisplayImePolicy(freeformConfig.displayId, 1)

        launchTargetAppOnDisplay(freeformConfig.displayId)
    }

    private fun setVirtualDisplayImePolicy(displayId: Int, policy: Int) {
        try {
            val windowManagerGlobalClass = Class.forName("android.view.WindowManagerGlobal")
            val getWmsMethod = windowManagerGlobalClass.getMethod("getWindowManagerService")
            val wms = getWmsMethod.invoke(null)
            val iWindowManagerClass = Class.forName("android.view.IWindowManager")
            val setPolicyMethod = iWindowManagerClass.getMethod("setDisplayImePolicy", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            setPolicyMethod.invoke(wms, displayId, policy)
            Log.d(TAG, "setDisplayImePolicy via standard reflection succeeded: displayId=$displayId, policy=$policy")
            return
        } catch (e: Exception) {
            Log.w(TAG, "Standard reflection failed for setDisplayImePolicy, trying HiddenApiBypass", e)
        }

        try {
            val wms = HiddenApiBypass.invoke(Class.forName("android.view.WindowManagerGlobal"), null, "getWindowManagerService")
            HiddenApiBypass.invoke(Class.forName("android.view.IWindowManager"), wms, "setDisplayImePolicy", displayId, policy)
            Log.d(TAG, "setDisplayImePolicy via HiddenApiBypass succeeded: displayId=$displayId, policy=$policy")
            return
        } catch (e: Exception) {
            Log.w(TAG, "HiddenApiBypass failed for setDisplayImePolicy, trying direct binder transact", e)
        }

        try {
            val smClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = smClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, "window") as? android.os.IBinder
            if (binder != null) {
                val data = android.os.Parcel.obtain()
                val reply = android.os.Parcel.obtain()
                try {
                    data.writeInterfaceToken("android.view.IWindowManager")
                    data.writeInt(displayId)
                    data.writeInt(policy)
                    binder.transact(108, data, reply, 0)
                    reply.readException()
                    Log.d(TAG, "setDisplayImePolicy via binder transact 108 succeeded: displayId=$displayId, policy=$policy")
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "All setDisplayImePolicy attempts failed for displayId=$displayId", e)
        }
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        surfaceTexture.setDefaultBufferSize(freeformConfig.freeformWidth, freeformConfig.freeformHeight)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        if (!hasRenderedFirstFrame) {
            hasRenderedFirstFrame = true
            splashView?.let { splash ->
                splash.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction {
                        try {
                            freeformRootView.removeView(splash)
                        } catch (_: Exception) {}
                        splashView = null
                    }
                    .start()
            }
        }
    }

    fun onUserTappedInsideApp() {
        subscreenDepth = (subscreenDepth + 1).coerceAtMost(8)
        Log.d(TAG, "onUserTappedInsideApp: subscreenDepth=$subscreenDepth")
    }

    private fun launchTargetAppOnDisplay(displayId: Int) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(freeformConfig.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                val options = ActivityOptions.makeBasic()
                options.setLaunchDisplayId(displayId)
                context.startActivity(intent, options.toBundle())
                Log.d(TAG, "App ${freeformConfig.packageName} launched onto display $displayId")
            } else {
                Log.e(TAG, "No launch intent for package: ${freeformConfig.packageName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app on virtual display", e)
        }
    }

    fun resizeVirtualDisplay(width: Int, height: Int, densityDpi: Int) {
        FreeformDisplayManager.resizeDisplay(virtualDisplay, width, height, densityDpi)
    }

    fun canNavigateBack(): Boolean {
        return subscreenDepth > 0
    }

    fun injectBackKey() {
        if (freeformConfig.displayId < 0) return
        if (!canNavigateBack()) {
            Log.d(TAG, "injectBackKey: Already on first screen (subscreenDepth=$subscreenDepth). Doing nothing.")
            return
        }
        subscreenDepth--
        Log.d(TAG, "injectBackKey: Injecting back key, remaining subscreenDepth=$subscreenDepth")
        FreeformInputTouchListener.injectBackKey(context, freeformConfig.displayId)
    }

    fun maximizeApp() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(freeformConfig.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val options = ActivityOptions.makeBasic()
                options.setLaunchDisplayId(Display.DEFAULT_DISPLAY)
                context.startActivity(intent, options.toBundle())
                Log.d(TAG, "Maximizing app to default display")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to maximize app", e)
        }
        // Destroy the floating window after a short delay so the main display can take over the activity seamlessly
        handler.postDelayed({
            destroy("maximizeApp")
        }, 300)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun handleHangUp() {
        if (freeformConfig.isHangUp) {
            windowParams.apply {
                x = freeformConfig.notInHangUpX
                y = freeformConfig.notInHangUpY
                flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            }
            freeformRootView.layoutParams.apply {
                width = freeformConfig.width
                height = freeformConfig.height
            }
            try {
                windowManager.updateViewLayout(freeformLayout, windowParams)
            } catch (_: Exception) {}
            topBarView.visibility = View.VISIBLE
            bottomBarView.visibility = View.VISIBLE
            freeformConfig.isHangUp = false
            freeformView.setOnTouchListener(FreeformInputTouchListener(context, this))
        } else {
            freeformConfig.notInHangUpX = windowParams.x
            freeformConfig.notInHangUpY = windowParams.y
            toHangUp()
            topBarView.visibility = View.GONE
            bottomBarView.visibility = View.GONE
            freeformConfig.isHangUp = true
            val gestureDetector = GestureDetector(context, hangUpGestureListener)
            freeformView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) makeSureFreeformInScreen()
                true
            }
        }
    }

    fun toHangUp() {
        windowParams.apply {
            x = (defaultDisplayWidth / 2 - freeformConfig.hangUpWidth / 2)
            y = -(defaultDisplayHeight / 2 - freeformConfig.hangUpHeight / 2)
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        freeformRootView.layoutParams = freeformRootView.layoutParams.apply {
            width = freeformConfig.hangUpWidth
            height = freeformConfig.hangUpHeight
        }
        try {
            windowManager.updateViewLayout(freeformLayout, windowParams)
        } catch (e: Exception) {
            Log.e(TAG, "toHangUp updateViewLayout error", e)
        }
    }

    fun makeSureFreeformInScreen() {
        if (!freeformConfig.isHangUp) {
            val maxWidth = defaultDisplayWidth
            val maxHeight = (defaultDisplayHeight * 0.95f).roundToInt()
            if (freeformRootView.layoutParams.width > maxWidth || freeformRootView.layoutParams.height > maxHeight) {
                freeformRootView.layoutParams = freeformRootView.layoutParams.apply {
                    width = min(freeformRootView.width, maxWidth)
                    height = min(freeformRootView.height, maxHeight)
                }
            }
        }
        val halfW = defaultDisplayWidth / 2
        val halfH = defaultDisplayHeight / 2
        if (windowParams.x < -halfW) FreeformAnimation.moveInScreenAnimator(windowParams.x, -halfW, 300, true, this)
        else if (windowParams.x > halfW) FreeformAnimation.moveInScreenAnimator(windowParams.x, halfW, 300, true, this)
        if (windowParams.y < -halfH) FreeformAnimation.moveInScreenAnimator(windowParams.y, -halfH, 300, false, this)
        else if (windowParams.y > halfH) FreeformAnimation.moveInScreenAnimator(windowParams.y, halfH, 300, false, this)
    }

    fun checkWindowOnTop() {
        if (getFreeformId() != FreeformWindowManager.topWindow) {
            handler.post {
                try {
                    windowManager.removeViewImmediate(freeformLayout)
                    windowManager.addView(freeformLayout, windowParams)
                    FreeformWindowManager.topWindow = getFreeformId()
                } catch (_: Exception) {}
            }
        }
    }

    fun getFreeformId(): String = freeformConfig.packageName

    fun destroy(callReason: String = "user") {
        if (isDestroyed) return
        isDestroyed = true
        Log.d(TAG, "destroy called: $callReason")

        handler.post {
            try {
                windowManager.removeViewImmediate(freeformLayout)
            } catch (e: Exception) {
                Log.e(TAG, "removeViewImmediate failed", e)
            }
        }

        FreeformDisplayManager.releaseDisplay(virtualDisplay)
        virtualDisplay = null

        surface?.release()
        surface = null

        FreeformWindowManager.removeWindow(getFreeformId())
        onCloseCallback(this)
    }
}
