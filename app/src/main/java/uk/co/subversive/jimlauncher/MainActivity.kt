package uk.co.subversive.jimlauncher

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val LAUNCHER_URL = "https://steady-bunny-526205.netlify.app/"
        private const val EXIT_PIN = "8228"
        private const val CORNER_HOLD_MS = 5000L  // 5 seconds
        private const val CORNER_ZONE_PX = 200    // 200px square in top-right
        private const val VOLUME_CAP_PERCENT = 85
    }

    private lateinit var webView: WebView
    private lateinit var rootLayout: FrameLayout
    private val handler = Handler(Looper.getMainLooper())
    private var cornerHoldRunnable: Runnable? = null
    private var screenOnReceiver: ScreenOnReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Window flags - keep screen on, show when locked, turn screen on when activity wakes
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Newer API equivalents (Android 8.1+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // Build the UI programmatically (no XML needed)
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = true
                allowContentAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = userAgentString + " JimLauncher/1.0"
            }
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl(LAUNCHER_URL)
        }

        rootLayout.addView(webView)
        setContentView(rootLayout)

        // Hide system UI - immersive sticky mode
        applyImmersiveMode()

        // Cap volume to 85%
        capVolume()
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        // Re-enter Lock Task Mode when activity resumes (e.g. after screen wake)
        startLockTaskIfPossible()
        capVolume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }

    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    private fun startLockTaskIfPossible() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            // Only enter Lock Task Mode if we're not already in it.
            // This stops the "App pinned" notification firing on every screen wake.
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask()
            }
        } catch (e: Exception) {
            // Lock Task Mode requires the user to enable Screen Pinning in Settings first
            // If it fails, app still works but escape via back+recents is possible
        }
    }

    private fun capVolume() {
        try {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val cappedVol = (maxVol * VOLUME_CAP_PERCENT / 100)
            val currentVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentVol > cappedVol) {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, cappedVol, 0)
            }
        } catch (e: Exception) {
            // No-op
        }
    }

    // ====================================================================
    // KEY EVENT HANDLING - intercept volume buttons to cap at 85%
    // ====================================================================
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                handleVolumeUp()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                handleVolumeDown()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                return true  // Block mute
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun handleVolumeUp() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cappedVol = (maxVol * VOLUME_CAP_PERCENT / 100)
        val currentVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVol < cappedVol) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol + 1, AudioManager.FLAG_SHOW_UI)
        }
        // If at cap, do nothing - silently blocked
    }

    private fun handleVolumeDown() {
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVol > 0) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol - 1, AudioManager.FLAG_SHOW_UI)
        }
    }

    // ====================================================================
    // BACK BUTTON - swallow it (don't let it close app)
    // ====================================================================
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing - back button is disabled
    }

    // ====================================================================
    // CORNER HOLD DETECTION - 5 second top-right hold triggers PIN dialog
    //
    // Implemented via dispatchTouchEvent at the Activity level so we always
    // see both ACTION_DOWN and ACTION_UP regardless of which child view ends
    // up handling the gesture. Previous approach used a non-consuming overlay
    // View which never received ACTION_UP and left runnables hanging in the
    // handler queue, causing the PIN dialog to fire 5s after any tap in the
    // corner zone (e.g. on the right drawer trigger button).
    // ====================================================================
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        handleCornerHold(event)
        return super.dispatchTouchEvent(event)
    }

    private fun handleCornerHold(event: MotionEvent) {
        val windowWidth = window.decorView.width
        if (windowWidth <= 0) return  // layout not ready yet

        val inCorner = event.x >= (windowWidth - CORNER_ZONE_PX) && event.y <= CORNER_ZONE_PX

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (inCorner) {
                    cancelCornerHold()  // defensive: clear any stale runnable
                    cornerHoldRunnable = Runnable { showPinDialog() }
                    handler.postDelayed(cornerHoldRunnable!!, CORNER_HOLD_MS)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // If the finger drifts out of the corner zone mid-gesture, cancel
                if (!inCorner) {
                    cancelCornerHold()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelCornerHold()
            }
        }
    }

    private fun cancelCornerHold() {
        cornerHoldRunnable?.let { handler.removeCallbacks(it) }
        cornerHoldRunnable = null
    }

    private fun showPinDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val title = TextView(this).apply {
            text = "Enter PIN to Exit"
            textSize = 20f
            setPadding(0, 0, 0, 30)
        }
        container.addView(title)

        val pinInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            imeOptions = EditorInfo.IME_ACTION_DONE
            hint = "4 digits"
            textSize = 24f
        }
        container.addView(pinInput)

        val dialog = AlertDialog.Builder(this)
            .setView(container)
            .setPositiveButton("Exit") { _, _ ->
                if (pinInput.text.toString() == EXIT_PIN) {
                    exitKiosk()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun exitKiosk() {
        try {
            stopLockTask()
        } catch (e: Exception) {
            // No-op
        }
        finish()
    }

    // ====================================================================
    // SCREEN ON RECEIVER - re-bring app to front when screen wakes
    // ====================================================================
    override fun onStart() {
        super.onStart()
        if (screenOnReceiver == null) {
            screenOnReceiver = ScreenOnReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenOnReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        screenOnReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) {}
            screenOnReceiver = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
