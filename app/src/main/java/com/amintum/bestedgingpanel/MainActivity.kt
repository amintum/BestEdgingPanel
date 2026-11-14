package com.amintum.bestedgingpanel

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.View.GONE
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.amintum.bestedgingpanel.databinding.ActivityMainM3Binding

class MainActivity : AppCompatActivity(), android.content.SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var binding: ActivityMainM3Binding
    private lateinit var panelPrefs: PanelPreferences

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainM3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        panelPrefs = PanelPreferences(this)

        setupListeners()
        registerPinnedShortcut()
    }

    private fun registerPinnedShortcut() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            
            val toggleIntent = Intent(this, ToggleActivity::class.java).apply {
                action = ToggleActivity.ACTION_TOGGLE
            }

            val shortcut = android.content.pm.ShortcutInfo.Builder(this, "toggle_sidebar")
                .setShortLabel("Toggle Sidebar")
                .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(toggleIntent)
                .build()

            try {
                shortcutManager.dynamicShortcuts = listOf(shortcut)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupListeners() {
        val toggleListener = View.OnClickListener { togglePanel() }
        binding.btnStartStop.setOnClickListener(toggleListener)
        binding.btnStartStopClassic.setOnClickListener(toggleListener)

        binding.btnTogglePanel.setOnClickListener { triggerPanelToggle() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsMainActivity::class.java))
        }
        binding.btnShowLogs.setOnClickListener {
            showLogsDialog()
        }

        binding.btnHowToUse.setOnClickListener {
            val isVisible = binding.layoutTutorialContent.visibility == View.VISIBLE
            if (isVisible) {
                binding.layoutTutorialContent.visibility = View.GONE
            } else {
                binding.layoutTutorialContent.visibility = View.VISIBLE
                binding.mainScrollView.post {
                    binding.mainScrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }

        // Load Tutorial Animation
        Glide.with(this)
            .asGif()
            .load(R.drawable.tutorial_anim)
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
            .into(binding.ivTutorialAnim)
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("side_panel_prefs", android.content.Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)
        
        applyHomeButtonStyle()
        updateServiceStatus()
    }

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("side_panel_prefs", android.content.Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
        if (key == "service_enabled") {
            runOnUiThread {
                updateServiceStatus()
            }
        }
    }

    private fun applyHomeButtonStyle() {
        val isPowerStyle = panelPrefs.homeButtonStyle == PanelPreferences.STYLE_POWER
        binding.btnStartStop.visibility = if (isPowerStyle) View.VISIBLE else View.GONE
        binding.btnStartStopClassic.visibility = if (isPowerStyle) GONE else View.VISIBLE
    }

    private fun updateServiceStatus() {
        val isEnabled = panelPrefs.serviceEnabled
        val typedValue = android.util.TypedValue()
        
        if (isEnabled) {
            // Service is fully ACTIVE (Green)
            binding.btnStartStop.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2ECC71"))
            binding.btnStartStop.setIconTintResource(android.R.color.white)
            
            binding.btnStartStopClassic.text = getString(R.string.action_stop)
            binding.btnStartStopClassic.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2ECC71"))
            binding.btnStartStopClassic.setTextColor(Color.WHITE)

            binding.tvStatus.text = getString(R.string.status_service_active)
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            binding.tvStatus.setTextColor(typedValue.data)
            binding.statusDot.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2ECC71"))
        } else {
            // Service is STOPPED (Slate)
            binding.btnStartStop.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#475569"))
            binding.btnStartStop.setIconTintResource(com.google.android.material.R.color.material_dynamic_neutral90)
            
            binding.btnStartStopClassic.text = getString(R.string.action_start)
            binding.btnStartStopClassic.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#475569"))
            binding.btnStartStopClassic.setTextColor(Color.WHITE)

            binding.tvStatus.text = getString(R.string.main_service_stopped)
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
            binding.tvStatus.setTextColor(typedValue.data)
            binding.statusDot.imageTintList = android.content.res.ColorStateList.valueOf(typedValue.data)
        }
        
        binding.btnStartStop.text = "" 
    }

    private fun showLogsDialog() {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val time = sdf.format(java.util.Date())
        
        val logBuilder = StringBuilder()
        logBuilder.append("[$time] Log Session Started\n")
        logBuilder.append("[$time] Device: ${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})\n")
        logBuilder.append("[$time] Privileged System App Mode\n")
        logBuilder.append("[$time] Service: ${if (FloatingPanelService.isRunning) "RUNNING" else "STOPPED"}\n")
        logBuilder.append("[$time] Theme: ${panelPrefs.uiTheme.uppercase()}\n")
        logBuilder.append("[$time] --- End of Summary ---")

        val tv = TextView(this).apply {
            text = logBuilder.toString()
            setPadding(64, 32, 64, 32)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val scroll = android.widget.ScrollView(this).apply {
            addView(tv)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.main_logs_title)
            .setView(scroll)
            .setPositiveButton(R.string.dialog_close, null)
            .show()
    }

    private fun togglePanel() {
        val activeBtn = if (panelPrefs.homeButtonStyle == PanelPreferences.STYLE_POWER) 
            binding.btnStartStop else binding.btnStartStopClassic
            
        activeBtn.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        SpringAnimator.scalePulse(activeBtn)

        panelPrefs.toggleService(this)
    }

    private fun triggerPanelToggle() {
        binding.root.showModernToast("Opening BestEdgingPanel...")
        val intent = Intent(this, FloatingPanelService::class.java).apply {
            action = FloatingPanelService.ACTION_OPEN
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
