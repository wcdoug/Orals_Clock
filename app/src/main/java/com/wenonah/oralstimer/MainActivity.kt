package com.wenonah.oralstimer

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class MainActivity : AppCompatActivity() {

    // ------- State -------
    private enum class State { IDLE, RUNNING, PAUSED, EXPIRED }
    private var state = State.IDLE

    // ------- Timer -------
    private var countDownTimer: CountDownTimer? = null
    private var selectedDurationMs: Long = 0L   // duration currently loaded
    private var remainingMs: Long = 0L          // used when resuming from pause

    // ------- Wake lock -------
    private var wakeLock: PowerManager.WakeLock? = null

    // ------- Views -------
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var tvCountdown: TextView
    private lateinit var btnPlayPause: Button
    private lateinit var btnReset: Button
    private lateinit var btn30: Button
    private lateinit var btn40: Button
    private lateinit var btn60: Button
    private lateinit var btn90: Button
    private lateinit var btn120: Button
    private lateinit var etCustomMinutes: EditText
    private lateinit var btnSet: Button

    // ------- Flash -------
    private val flashHandler = Handler(Looper.getMainLooper())
    private var flashCount = 0
    private val FLASH_TOTAL = 20    // 10 full on/off cycles
    private val FLASH_INTERVAL_MS = 300L

    // ------- Colors (resolved at runtime) -------
    private var colorBackground = 0
    private var colorWarningYellow = 0
    private var colorCriticalRed = 0
    private var colorFlashWhite = 0
    private var colorTimerTextDefault = 0
    private var colorTimerTextDark = 0

    // ------- Thresholds -------
    private val WARNING_MS = 5 * 60 * 1000L   // 5 minutes
    private val CRITICAL_MS = 1 * 60 * 1000L  // 1 minute

    // =========================================================
    // Lifecycle
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on at the window level (belt-and-suspenders with wake lock)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Lock Task Mode — pins the app as a kiosk when this app is the Device Owner.
        // Set up once via ADB: adb shell dpm set-device-owner com.wenonah.oralstimer/.AdminReceiver
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isLockTaskPermitted(packageName)) {
            startLockTask()
        }

        setContentView(R.layout.activity_main)

        // Hide system navigation bar and status bar (immersive sticky)
        hideSystemUI()

        // Resolve colors
        colorBackground     = getColor(R.color.colorBackground)
        colorWarningYellow  = getColor(R.color.colorWarningYellow)
        colorCriticalRed    = getColor(R.color.colorCriticalRed)
        colorFlashWhite     = getColor(R.color.colorFlashWhite)
        colorTimerTextDefault = getColor(R.color.colorTimerTextDefault)
        colorTimerTextDark  = getColor(R.color.colorTimerTextDark)

        // Bind views
        rootLayout      = findViewById(R.id.rootLayout)
        tvCountdown     = findViewById(R.id.tvCountdown)
        btnPlayPause    = findViewById(R.id.btnPlayPause)
        btnReset        = findViewById(R.id.btnReset)
        btn30           = findViewById(R.id.btn30)
        btn40           = findViewById(R.id.btn40)
        btn60           = findViewById(R.id.btn60)
        btn90           = findViewById(R.id.btn90)
        btn120          = findViewById(R.id.btn120)
        etCustomMinutes = findViewById(R.id.etCustomMinutes)
        btnSet          = findViewById(R.id.btnSet)

        // Preset listeners
        btn30.setOnClickListener  { loadPreset(30) }
        btn40.setOnClickListener  { loadPreset(40) }
        btn60.setOnClickListener  { loadPreset(60) }
        btn90.setOnClickListener  { loadPreset(90) }
        btn120.setOnClickListener { loadPreset(120) }

        // Custom Set listener
        btnSet.setOnClickListener { loadCustomMinutes() }

        // Play/Pause listener
        btnPlayPause.setOnClickListener {
            when (state) {
                State.IDLE    -> if (selectedDurationMs > 0) startTimer(selectedDurationMs)
                State.RUNNING -> pauseTimer()
                State.PAUSED  -> startTimer(remainingMs)
                State.EXPIRED -> { /* no-op, user must reset first */ }
            }
        }

        // Reset listener
        btnReset.setOnClickListener { resetTimer() }

        // Initial UI state
        updateDisplay(0L)
        setBackground(colorBackground, colorTimerTextDefault)
        btnPlayPause.text = getString(R.string.btn_play)
        btnPlayPause.isEnabled = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        flashHandler.removeCallbacksAndMessages(null)
        releaseWakeLock()
    }

    // =========================================================
    // Preset & Custom loading
    // =========================================================

    private fun loadPreset(minutes: Int) {
        stopAndReset()
        selectedDurationMs = minutes * 60 * 1000L
        remainingMs = selectedDurationMs
        updateDisplay(selectedDurationMs)
        setBackground(colorBackground, colorTimerTextDefault)
        btnPlayPause.isEnabled = true
        btnPlayPause.text = getString(R.string.btn_play)
        state = State.IDLE
    }

    private fun loadCustomMinutes() {
        val text = etCustomMinutes.text.toString().trim()
        val minutes = text.toIntOrNull()
        if (minutes == null || minutes < 1 || minutes > 999) {
            etCustomMinutes.error = "1–999"
            return
        }
        etCustomMinutes.error = null
        // Dismiss keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etCustomMinutes.windowToken, 0)

        stopAndReset()
        selectedDurationMs = minutes * 60 * 1000L
        remainingMs = selectedDurationMs
        updateDisplay(selectedDurationMs)
        setBackground(colorBackground, colorTimerTextDefault)
        btnPlayPause.isEnabled = true
        btnPlayPause.text = getString(R.string.btn_play)
        state = State.IDLE
    }

    // =========================================================
    // Timer control
    // =========================================================

    private fun startTimer(durationMs: Long) {
        countDownTimer?.cancel()
        acquireWakeLock()

        countDownTimer = object : CountDownTimer(durationMs, 1000L) {

            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                updateDisplay(millisUntilFinished)
                applyColorForRemaining(millisUntilFinished)
            }

            override fun onFinish() {
                remainingMs = 0L
                updateDisplay(0L)
                state = State.EXPIRED
                btnPlayPause.isEnabled = false
                btnPlayPause.text = getString(R.string.btn_play)
                releaseWakeLock()
                startFlash()
            }
        }.start()

        state = State.RUNNING
        btnPlayPause.text = getString(R.string.btn_pause)
        btnPlayPause.isEnabled = true
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        state = State.PAUSED
        btnPlayPause.text = getString(R.string.btn_play)
        releaseWakeLock()
    }

    private fun resetTimer() {
        stopAndReset()
        if (selectedDurationMs > 0) {
            remainingMs = selectedDurationMs
            updateDisplay(selectedDurationMs)
            btnPlayPause.isEnabled = true
        } else {
            updateDisplay(0L)
            btnPlayPause.isEnabled = false
        }
        setBackground(colorBackground, colorTimerTextDefault)
        btnPlayPause.text = getString(R.string.btn_play)
        state = State.IDLE
    }

    /** Cancel timer + flash without touching selectedDurationMs */
    private fun stopAndReset() {
        countDownTimer?.cancel()
        countDownTimer = null
        flashHandler.removeCallbacksAndMessages(null)
        releaseWakeLock()
    }

    // =========================================================
    // Display helpers
    // =========================================================

    private fun updateDisplay(ms: Long) {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        tvCountdown.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun applyColorForRemaining(ms: Long) {
        when {
            ms <= CRITICAL_MS -> setBackground(colorCriticalRed, colorTimerTextDefault)
            ms <= WARNING_MS  -> setBackground(colorWarningYellow, colorTimerTextDark)
            else              -> setBackground(colorBackground, colorTimerTextDefault)
        }
    }

    private fun setBackground(bgColor: Int, textColor: Int) {
        rootLayout.setBackgroundColor(bgColor)
        tvCountdown.setTextColor(textColor)
    }

    // =========================================================
    // Screen flash on expiry
    // =========================================================

    private fun startFlash() {
        flashCount = 0
        flashStep()
    }

    private fun flashStep() {
        if (flashCount >= FLASH_TOTAL) {
            // End of flash — settle on red to indicate expired
            setBackground(colorCriticalRed, colorTimerTextDefault)
            return
        }
        val isEven = flashCount % 2 == 0
        setBackground(
            if (isEven) colorFlashWhite else colorCriticalRed,
            if (isEven) colorTimerTextDark else colorTimerTextDefault
        )
        flashCount++
        flashHandler.postDelayed({ flashStep() }, FLASH_INTERVAL_MS)
    }

    // =========================================================
    // Wake lock helpers
    // =========================================================

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
            "OralsTImer::TimerWakeLock"
        ).also {
            it.acquire(3 * 60 * 60 * 1000L) // max 3 hours safety ceiling
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }
}
