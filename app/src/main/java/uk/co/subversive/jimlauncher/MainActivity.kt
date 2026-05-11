package uk.co.subversive.jimlauncher

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class MainActivity : Activity() {

    companion object {
        // TODO: Replace this with Jared's actual Netlify URL before pushing the commit
        private const val LAUNCHER_URL = "https://bespoke-elf-86cb9c.netlify.app/"
        private const val VOLUME_CAP_PERCENT = 65
    }

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while watching - useful for long video sessions
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val rootLayout = FrameLayout(this).apply {
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
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = userAgentString + " JaredLauncher/1.0"
            }
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl(LAUNCHER_URL)
        }

        rootLayout.addView(webView)
        setContentView(rootLayout)

        applyImmersiveMode()
        capVolume()
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        capVolume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
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

    // ====================================================================
    // VOLUME CAP - the whole point of this APK
    // ====================================================================
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val cappedVol = (maxVol * VOLUME_CAP_PERCENT / 100)
                val currentVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVol < cappedVol) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol + 1, AudioManager.FLAG_SHOW_UI)
                }
                // If at cap, do nothing - silently blocked
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val currentVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVol > 0) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol - 1, AudioManager.FLAG_SHOW_UI)
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
