package com.Taybetibrowser

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var webrtcBlock: Boolean
        get() = prefs.getBoolean("webrtc_block", false)
        set(value) = prefs.edit().putBoolean("webrtc_block", value).apply()

    var canvasNoise: Boolean
        get() = prefs.getBoolean("canvas_noise", false)
        set(value) = prefs.edit().putBoolean("canvas_noise", value).apply()

    var spoofUserAgent: Boolean
        get() = prefs.getBoolean("spoof_user_agent", false)
        set(value) = prefs.edit().putBoolean("spoof_user_agent", value).apply()

    var stealthMode: Boolean
        get() = prefs.getBoolean("stealth_mode", false)
        set(value) = prefs.edit().putBoolean("stealth_mode", value).apply()

    var referrerPolicy: Boolean
        get() = prefs.getBoolean("referrer_policy", false)
        set(value) = prefs.edit().putBoolean("referrer_policy", value).apply()

    var httpsUpgrade: Boolean
        get() = prefs.getBoolean("https_upgrade", false)
        set(value) = prefs.edit().putBoolean("https_upgrade", value).apply()

    var trackingProtection: String
        get() = prefs.getString("tracking_protection", "STRICT") ?: "STRICT"
        set(value) = prefs.edit().putString("tracking_protection", value).apply()

    var dohEnabled: Boolean
        get() = prefs.getBoolean("doh", false)
        set(value) = prefs.edit().putBoolean("doh", value).apply()

    var dohProvider: String
        get() = prefs.getString("doh_provider", "CLOUDFLARE") ?: "CLOUDFLARE"
        set(value) = prefs.edit().putString("doh_provider", value).apply()

    var httpsOnly: Boolean
        get() = prefs.getBoolean("https_only", false)
        set(value) = prefs.edit().putBoolean("https_only", value).apply()

    var jsEnabled: Boolean
        get() = prefs.getBoolean("js_enabled", true)
        set(value) = prefs.edit().putBoolean("js_enabled", value).apply()

    var secureKeyboard: Boolean
        get() = prefs.getBoolean("secure_keyboard", true)
        set(value) = prefs.edit().putBoolean("secure_keyboard", value).apply()

    var keyRandomization: Boolean
        get() = prefs.getBoolean("key_randomization", false)
        set(value) = prefs.edit().putBoolean("key_randomization", value).apply()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean("haptic_feedback", false)
        set(value) = prefs.edit().putBoolean("haptic_feedback", value).apply()

    var pasteButton: Boolean
        get() = prefs.getBoolean("paste_button", false)
        set(value) = prefs.edit().putBoolean("paste_button", value).apply()

    var copyButton: Boolean
        get() = prefs.getBoolean("copy_button", false)
        set(value) = prefs.edit().putBoolean("copy_button", value).apply()

    var clearOnExit: Boolean
        get() = prefs.getBoolean("clear_on_exit", true)
        set(value) = prefs.edit().putBoolean("clear_on_exit", value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var vpnEnabled: Boolean
        get() = prefs.getBoolean("vpn_enabled", false)
        set(value) = prefs.edit().putBoolean("vpn_enabled", value).apply()

    var adBlockEnabled: Boolean
        get() = prefs.getBoolean("adblock_enabled", true)
        set(value) = prefs.edit().putBoolean("adblock_enabled", value).apply()

    companion object {
        const val WEBRTC_API_SCRIPT = """
            (function() {
                if (!window.RTCPeerConnection && !window.webkitRTCPeerConnection) {
                    Object.defineProperty(window, 'RTCPeerConnection', {'value': undefined});
                    Object.defineProperty(window, 'webkitRTCPeerConnection', {'value': undefined});
                }
                if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                    const originalGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
                    navigator.mediaDevices.getUserMedia = function(constraints) {
                        return Promise.reject(new Error('getUserMedia disabled'));
                    };
                }
            })();
        """

        const val CANVAS_NOISE_SCRIPT = """
            (function() {
                const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
                const originalGetImageData = CanvasRenderingContext2D.prototype.getImageData;
                let noiseCount = 0;
                HTMLCanvasElement.prototype.toDataURL = function() {
                    if (noiseCount++ %% 3 === 0) {
                        const ctx = this.getContext('2d');
                        if (ctx) {
                            const imageData = ctx.getImageData(0, 0, this.width, this.height);
                            for (let i = 0; i < imageData.data.length; i += 4) {
                                imageData.data[i] += Math.floor(Math.random() * 10) - 5;
                                imageData.data[i + 1] += Math.floor(Math.random() * 10) - 5;
                                imageData.data[i + 2] += Math.floor(Math.random() * 10) - 5;
                            }
                            ctx.putImageData(imageData, 0, 0);
                        }
                    }
                    return originalToDataURL.apply(this, arguments);
                };
                CanvasRenderingContext2D.prototype.getImageData = function() {
                    const result = originalGetImageData.apply(this, arguments);
                    for (let i = 0; i < result.data.length; i += 4) {
                        result.data[i] += Math.floor(Math.random() * 10) - 5;
                        result.data[i + 1] += Math.floor(Math.random() * 10) - 5;
                        result.data[i + 2] += Math.floor(Math.random() * 10) - 5;
                    }
                    return result;
                };
            })();
        """

        const val STEALTH_SCRIPT = """
            (function() {
                if (window.AndroidKeyboard) {
                    for (let key in window.AndroidKeyboard) {
                        if (typeof window.AndroidKeyboard[key] === 'function') {
                            (function(k) {
                                window.AndroidKeyboard[k] = function() {
                                    return null;
                                };
                            })(key);
                        }
                    }
                }
            })();
        """

        const val REFERRER_POLICY_SCRIPT = """
            (function() {
                const meta = document.createElement('meta');
                meta.name = 'referrer';
                meta.content = 'same-origin';
                document.head.appendChild(meta);
            })();
        """

        fun getAntiFingerprintScripts(prefs: PreferencesManager): String {
            val scripts = StringBuilder()
            scripts.append("if (window.RTCPeerConnection || window.webkitRTCPeerConnection) { ${WEBRTC_API_SCRIPT} }")
            scripts.append(CANVAS_NOISE_SCRIPT)
            scripts.append(REFERRER_POLICY_SCRIPT)
            return scripts.toString()
        }
    }
}