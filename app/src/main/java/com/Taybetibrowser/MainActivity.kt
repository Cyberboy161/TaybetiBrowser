package com.Taybetibrowser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.Taybetibrowser.keyboard.SecureKeyboardView

class MainActivity : AppCompatActivity() {

    private lateinit var webView: NoSystemKeyboardWebView
    private lateinit var urlBar: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var secureKeyboard: SecureKeyboardView

    private var isKeyboardVisible = false

    @SuppressLint("SetJavaScriptEnabled", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_main)

        urlBar = findViewById(R.id.url_bar)
        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnRefresh = findViewById(R.id.btn_refresh)
        webView = findViewById(R.id.web_view)
        secureKeyboard = findViewById(R.id.secure_keyboard)

        secureKeyboard.setHapticFeedback(false)

        setupWebView()
        setupUrlBar()
        setupButtons()
    }

    private fun setupWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectKeyboardDisabler()
                url?.let {
                    if (!it.startsWith("about:")) {
                        urlBar.setText(it.replace("https://", "").replace("http://", ""))
                    }
                }
            }
        }

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        webView.addJavascriptInterface(WebAppInterface(), "AndroidKeyboard")

        webView.loadUrl("https://duckduckgo.com")
    }

    private fun injectKeyboardDisabler() {
        webView.evaluateJavascript("""
            (function() {
                document.addEventListener('click', function(e) {
                    var el = e.target;
                    while (el) {
                        if ((el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') && !el.readOnly) {
                            var id = el.id || ('web_inp_' + Math.random().toString(36).substr(2, 9));
                            if (!el.id) el.id = id;
                            window.AndroidKeyboard.requestInputFocus(id);
                            break;
                        }
                        el = el.parentElement;
                    }
                }, true);

                document.addEventListener('touchstart', function(e) {
                    var el = e.target;
                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                        e.preventDefault();
                        var id = el.id || ('web_inp_' + Math.random().toString(36).substr(2, 9));
                        if (!el.id) el.id = id;
                        window.AndroidKeyboard.requestInputFocus(id);
                    }
                }, {passive: false});

                document.addEventListener('touchstart', function(e) {
                    var el = e.target;
                    if (el.isContentEditable && !window.getSelection().toString()) {
                        e.preventDefault();
                        var id = el.id || ('web_inp_' + Math.random().toString(36).substr(2, 9));
                        if (!el.id) el.id = id;
                        window.AndroidKeyboard.requestInputFocus(id);
                    }
                }, {passive: false});
            })();
        """.trimIndent(), null)
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun requestInputFocus(elementId: String) {
            runOnUiThread {
                hideSystemKeyboard()
                secureKeyboard.setWebViewTarget(webView, elementId)
                secureKeyboard.show()
                isKeyboardVisible = true
            }
        }
    }

    private fun hideSystemKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUrlBar() {
        urlBar.isFocusable = true
        urlBar.isFocusableInTouchMode = true

        urlBar.setOnClickListener {
            if (!urlBar.hasFocus()) {
                urlBar.requestFocus()
            }
            hideSystemKeyboard()
            secureKeyboard.setTargetEditText(urlBar)
            secureKeyboard.show()
            isKeyboardVisible = true
        }

        urlBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideSystemKeyboard()
                secureKeyboard.setTargetEditText(urlBar)
                secureKeyboard.show()
                isKeyboardVisible = true
            } else {
                secureKeyboard.hide()
                isKeyboardVisible = false
            }
        }

        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val input = urlBar.text.toString().trim()
                if (input.isNotEmpty()) {
                    val url = if (!input.contains("://")) {
                        if (input.contains(".") && !input.contains(" ")) {
                            "https://$input"
                        } else {
                            "https://www.google.com/search?q=${input.replace(" ", "+")}"
                        }
                    } else {
                        input
                    }
                    webView.loadUrl(url)
                }
                urlBar.clearFocus()
                hideSystemKeyboard()
                true
            } else {
                false
            }
        }

        urlBar.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (!urlBar.hasFocus()) {
                    urlBar.requestFocus()
                }
                hideSystemKeyboard()
                secureKeyboard.setTargetEditText(urlBar)
                secureKeyboard.show()
                isKeyboardVisible = true
            }
            false
        }
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        btnRefresh.setOnClickListener {
            webView.reload()
        }
    }

    override fun onBackPressed() {
        if (isKeyboardVisible) {
            secureKeyboard.hide()
            isKeyboardVisible = false
            return
        }
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

class NoSystemKeyboardWebView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    override fun onCreateInputConnection(editorInfo: EditorInfo): android.view.inputmethod.InputConnection? {
        return null
    }

    override fun onCheckIsTextEditor(): Boolean = true
}