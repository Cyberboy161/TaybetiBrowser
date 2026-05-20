package com.Taybetibrowser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Taybetibrowser.keyboard.SecureKeyboardView

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

class MainActivity : AppCompatActivity() {

    private lateinit var webView: NoSystemKeyboardWebView
    private lateinit var urlBar: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var btnBookmark: ImageButton
    private lateinit var btnNewTab: ImageButton
    private lateinit var tabsContainer: HorizontalScrollView
    private lateinit var tabsRow: LinearLayout
    private lateinit var secureKeyboard: SecureKeyboardView
    private lateinit var prefs: PreferencesManager
    private lateinit var db: com.Taybetibrowser.data.DatabaseHelper
    lateinit var internalClipboard: com.Taybetibrowser.data.InternalClipboard

    private var isKeyboardVisible = false
    private var jsInterfaceName = "AndroidKeyboard"
    private val tabs = mutableListOf<Tab>()
    private var currentTabId = 0

    data class Tab(val id: Int, val url: String, val title: String)

    @SuppressLint("SetJavaScriptEnabled", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_main)

        prefs = PreferencesManager(this)
        db = com.Taybetibrowser.data.DatabaseHelper(this)
        internalClipboard = com.Taybetibrowser.data.InternalClipboard(this)

        urlBar = findViewById(R.id.url_bar)
        btnBack = findViewById(R.id.btn_back)
        btnForward = findViewById(R.id.btn_forward)
        btnRefresh = findViewById(R.id.btn_refresh)
        btnMenu = findViewById(R.id.btn_menu)
        btnBookmark = findViewById(R.id.btn_bookmark)
        btnNewTab = findViewById(R.id.btn_new_tab)
        tabsContainer = findViewById(R.id.tabs_container)
        tabsRow = findViewById(R.id.tabs_row)
        webView = findViewById(R.id.web_view)
        secureKeyboard = findViewById(R.id.secure_keyboard)

        secureKeyboard.setHapticFeedback(prefs.hapticFeedback)
        secureKeyboard.setKeyRandomization(prefs.keyRandomization)
        secureKeyboard.setPasteEnabled(prefs.pasteButton)
        secureKeyboard.setCopyEnabled(prefs.copyButton)

        jsInterfaceName = if (prefs.stealthMode) "_ak${(Math.random() * 1000).toInt()}" else "AndroidKeyboard"

        setupWebView()
        setupUrlBar()
        setupButtons()
        setupMenu()
        applyTheme()
        if (tabs.isEmpty()) {
            tabs.add(Tab(0, "https://duckduckgo.com", "New Tab"))
        }
    }

    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        if (prefs.spoofUserAgent) {
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.144 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectPrivacyScripts()
                injectKeyboardHandler()
                checkAndAutoFillPassword(url)
                url?.let {
                    if (!it.startsWith("about:")) {
                        urlBar.setText(it.replace("https://", "").replace("http://", ""))
                        db.addHistoryEntry(it, view?.title ?: "")
                        tabs.indexOfFirst { t -> t.id == currentTabId }.let { idx ->
                            if (idx >= 0) {
                                tabs[idx] = tabs[idx].copy(url = it, title = view?.title ?: "Tab")
                            }
                        }
                    }
                }
            }
        }

        webView.addJavascriptInterface(WebAppInterface(), jsInterfaceName)

        webView.loadUrl("https://duckduckgo.com")
    }

    private fun injectPrivacyScripts() {
        val scripts = StringBuilder()

        if (prefs.webrtcBlock) {
            scripts.append(PreferencesManager.WEBRTC_API_SCRIPT)
        }

        if (prefs.canvasNoise) {
            scripts.append(PreferencesManager.CANVAS_NOISE_SCRIPT)
        }

        if (prefs.referrerPolicy) {
            scripts.append(PreferencesManager.REFERRER_POLICY_SCRIPT)
        }

        if (prefs.stealthMode) {
            scripts.append(PreferencesManager.STEALTH_SCRIPT)
        }

        if (scripts.isNotEmpty()) {
            webView.evaluateJavascript("""
                (function() {
                    try {
                        $scripts
                    } catch(e) {}
                })();
            """.trimIndent(), null)
        }
    }

    private fun injectKeyboardHandler() {
        webView.evaluateJavascript("""
            (function() {
                function findInput(el) {
                    while (el && el.tagName !== 'BODY') {
                        if ((el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') && !el.readOnly && el.type !== 'hidden') {
                            return el;
                        }
                        if (el.isContentEditable && el.contentEditable !== 'false') {
                            return el;
                        }
                        el = el.parentElement;
                    }
                    return null;
                }

                document.addEventListener('click', function(e) {
                    var input = findInput(e.target);
                    if (input && input.id) {
                        window.$jsInterfaceName.requestInputFocus(input.id);
                    }
                }, true);

                document.addEventListener('touchstart', function(e) {
                    var input = findInput(e.target);
                    if (input) {
                        e.preventDefault();
                        var id = input.id || 'inp_' + Math.random().toString(36).substr(2, 9);
                        if (!input.id) input.id = id;
                        window.$jsInterfaceName.requestInputFocus(id);
                    }
                }, {passive: false});

                document.addEventListener('selectionchange', function(e) {
                    var selection = window.getSelection();
                    if (selection && selection.toString().length > 0) {
                        var selectedText = selection.toString();
                        if (selectedText.length > 0) {
                            window.$jsInterfaceName.onTextSelected(selectedText);
                        }
                    }
                });

                document.addEventListener('touchend', function(e) {
                    setTimeout(function() {
                        var selection = window.getSelection();
                        if (selection && selection.toString().length > 0) {
                            var selectedText = selection.toString();
                            if (selectedText.length > 0) {
                                window.$jsInterfaceName.onTextSelected(selectedText);
                            }
                        }
                    }, 300);
                });
            })();
        """.trimIndent(), null)
    }

    private fun checkAndAutoFillPassword(url: String?) {
        if (url.isNullOrEmpty()) return
        val passwordEntry = db.getPasswordForUrl(url)
        passwordEntry?.let { entry ->
            webView.evaluateJavascript("""
                (function() {
                    var inputs = document.querySelectorAll('input[type="text"], input[type="email"], input[name="username"], input[id*="user"], input[id*="email"]');
                    for (var i = 0; i < inputs.length; i++) {
                        if (inputs[i].offsetParent !== null) {
                            inputs[i].value = '${entry.username.replace("'", "\\'")}';
                            inputs[i].dispatchEvent(new Event('input', {bubbles: true}));
                            break;
                        }
                    }
                    var passwords = document.querySelectorAll('input[type="password"], input[name="password"], input[id*="pass"]');
                    for (var i = 0; i < passwords.length; i++) {
                        if (passwords[i].offsetParent !== null) {
                            passwords[i].value = '${entry.password.replace("'", "\\'")}';
                            passwords[i].dispatchEvent(new Event('input', {bubbles: true}));
                            break;
                        }
                    }
                })();
            """.trimIndent(), null)
        }
    }

    private fun setupMenu() {
        btnMenu.setOnClickListener { view ->
            showMainMenu(view)
        }
    }

    private fun showMainMenu(anchor: View) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.menu_layout, null)

        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 8f
        popup.setBackgroundDrawable(getDrawable(R.color.surface))

        val themeStatus = popupView.findViewById<TextView>(R.id.theme_status)
        themeStatus.text = if (prefs.darkTheme) "Dark" else "Light"

        val tabsCount = popupView.findViewById<TextView>(R.id.tabs_count)
        tabsCount.text = " (${tabs.size})"

        val vpnStatus = popupView.findViewById<TextView>(R.id.vpn_status)
        vpnStatus.text = if (prefs.vpnEnabled) "On" else "Off"

        val adblockStatus = popupView.findViewById<TextView>(R.id.adblock_status)
        adblockStatus.text = if (prefs.adBlockEnabled) "On" else "Off"
        adblockStatus.setTextColor(getColor(if (prefs.adBlockEnabled) R.color.secure else R.color.text_secondary))

        popupView.findViewById<LinearLayout>(R.id.menu_tabs).setOnClickListener {
            popup.dismiss()
            showTabsMenu(anchor)
        }

        popupView.findViewById<LinearLayout>(R.id.menu_bookmarks).setOnClickListener {
            popup.dismiss()
            showBookmarksMenu(anchor)
        }

        popupView.findViewById<LinearLayout>(R.id.menu_passwords).setOnClickListener {
            popup.dismiss()
            showPasswordsMenu(anchor)
        }

        popupView.findViewById<LinearLayout>(R.id.menu_theme).setOnClickListener {
            prefs.darkTheme = !prefs.darkTheme
            popup.dismiss()
            applyTheme()
        }

        popupView.findViewById<LinearLayout>(R.id.menu_history).setOnClickListener {
            popup.dismiss()
            showHistoryMenu(anchor)
        }

        popupView.findViewById<LinearLayout>(R.id.menu_privacy).setOnClickListener {
            popup.dismiss()
            showPrivacyMenu(anchor)
        }

        popupView.findViewById<LinearLayout>(R.id.menu_clipboard).setOnClickListener {
            popup.dismiss()
            showClipboardMenu(anchor)
        }

        popupView.findViewById<LinearLayout>(R.id.menu_downloads).setOnClickListener {
            popup.dismiss()
            showDownloadsMenu(anchor)
        }

        popupView.findViewById<LinearLayout>(R.id.menu_vpn).setOnClickListener {
            prefs.vpnEnabled = !prefs.vpnEnabled
            vpnStatus.text = if (prefs.vpnEnabled) "On" else "Off"
        }

        popupView.findViewById<LinearLayout>(R.id.menu_adblock).setOnClickListener {
            prefs.adBlockEnabled = !prefs.adBlockEnabled
            adblockStatus.text = if (prefs.adBlockEnabled) "On" else "Off"
            adblockStatus.setTextColor(getColor(if (prefs.adBlockEnabled) R.color.secure else R.color.text_secondary))
        }

        popup.showAtLocation(anchor, Gravity.TOP or Gravity.END, 16, 80)
    }

    private fun applyTheme() {
        if (prefs.darkTheme) {
            delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private fun showBookmarksMenu(anchor: View) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.bookmarks_menu, null)

        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 8f
        popup.setBackgroundDrawable(getDrawable(R.color.surface))

        val titleInput = popupView.findViewById<EditText>(R.id.input_title)
        val urlInput = popupView.findViewById<EditText>(R.id.input_url)
        val addBtn = popupView.findViewById<ImageButton>(R.id.btn_add)

        val bookmarksList = popupView.findViewById<RecyclerView>(R.id.bookmarks_list)
        bookmarksList.layoutManager = LinearLayoutManager(this)

        val adapter = BookmarkAdapter(db.getAllBookmarks(),
            onClick = { bookmark ->
                popup.dismiss()
                webView.loadUrl(bookmark.url)
            },
            onDelete = { bookmark ->
                db.deleteBookmark(bookmark.id)
                bookmarksList.adapter?.let { (it as BookmarkAdapter).updateData(db.getAllBookmarks()) }
            }
        )
        bookmarksList.adapter = adapter

        addBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val url = urlInput.text.toString().trim()
            if (title.isNotEmpty() && url.isNotEmpty()) {
                db.addBookmark(title, url)
                titleInput.text.clear()
                urlInput.text.clear()
                adapter.updateData(db.getAllBookmarks())
            }
        }

        popup.showAtLocation(anchor, Gravity.CENTER, 0, 0)
    }

    private fun showPasswordsMenu(anchor: View) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.passwords_menu, null)

        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 8f
        popup.setBackgroundDrawable(getDrawable(R.color.surface))

        val titleInput = popupView.findViewById<EditText>(R.id.input_title)
        val urlInput = popupView.findViewById<EditText>(R.id.input_url)
        val userInput = popupView.findViewById<EditText>(R.id.input_username)
        val passInput = popupView.findViewById<EditText>(R.id.input_password)
        val addBtn = popupView.findViewById<ImageButton>(R.id.btn_add)

        addBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val url = urlInput.text.toString().trim()
            val username = userInput.text.toString().trim()
            val password = passInput.text.toString().trim()
            if (title.isNotEmpty() && url.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                db.addPassword(title, url, username, password)
                titleInput.text.clear()
                urlInput.text.clear()
                userInput.text.clear()
                passInput.text.clear()
            }
        }

        popup.showAtLocation(anchor, Gravity.CENTER, 0, 0)
    }

    private fun showPrivacyMenu(anchor: View) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.privacy_menu_layout, null)

        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 8f
        popup.setBackgroundDrawable(getDrawable(R.color.surface))

        val webrtcStatus = popupView.findViewById<TextView>(R.id.webrtc_status)
        val canvasStatus = popupView.findViewById<TextView>(R.id.canvas_status)
        val referrerStatus = popupView.findViewById<TextView>(R.id.referrer_status)
        val stealthStatus = popupView.findViewById<TextView>(R.id.stealth_status)
        val useragentStatus = popupView.findViewById<TextView>(R.id.useragent_status)
        val keyboardStatus = popupView.findViewById<TextView>(R.id.keyboard_status)
        val hapticStatus = popupView.findViewById<TextView>(R.id.haptic_status)
        val randomStatus = popupView.findViewById<TextView>(R.id.random_status)
        val pasteStatus = popupView.findViewById<TextView>(R.id.paste_status)
        val copyStatus = popupView.findViewById<TextView>(R.id.copy_status)

        fun updateStatus(textView: TextView, enabled: Boolean) {
            textView.text = if (enabled) "On" else "Off"
            textView.setTextColor(getColor(if (enabled) R.color.secure else R.color.text_secondary))
        }

        updateStatus(webrtcStatus, prefs.webrtcBlock)
        updateStatus(canvasStatus, prefs.canvasNoise)
        updateStatus(referrerStatus, prefs.referrerPolicy)
        updateStatus(stealthStatus, prefs.stealthMode)
        updateStatus(useragentStatus, prefs.spoofUserAgent)
        updateStatus(keyboardStatus, prefs.secureKeyboard)
        updateStatus(hapticStatus, prefs.hapticFeedback)
        updateStatus(randomStatus, prefs.keyRandomization)
        updateStatus(pasteStatus, prefs.pasteButton)
        updateStatus(copyStatus, prefs.copyButton)

        popupView.findViewById<LinearLayout>(R.id.privacy_back).setOnClickListener {
            popup.dismiss()
            showMainMenu(anchor)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_webrtc).setOnClickListener {
            prefs.webrtcBlock = !prefs.webrtcBlock
            updateStatus(webrtcStatus, prefs.webrtcBlock)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_canvas).setOnClickListener {
            prefs.canvasNoise = !prefs.canvasNoise
            updateStatus(canvasStatus, prefs.canvasNoise)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_referrer).setOnClickListener {
            prefs.referrerPolicy = !prefs.referrerPolicy
            updateStatus(referrerStatus, prefs.referrerPolicy)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_stealth).setOnClickListener {
            prefs.stealthMode = !prefs.stealthMode
            updateStatus(stealthStatus, prefs.stealthMode)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_useragent).setOnClickListener {
            prefs.spoofUserAgent = !prefs.spoofUserAgent
            updateStatus(useragentStatus, prefs.spoofUserAgent)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_keyboard).setOnClickListener {
            prefs.secureKeyboard = !prefs.secureKeyboard
            updateStatus(keyboardStatus, prefs.secureKeyboard)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_haptic).setOnClickListener {
            prefs.hapticFeedback = !prefs.hapticFeedback
            secureKeyboard.setHapticFeedback(prefs.hapticFeedback)
            updateStatus(hapticStatus, prefs.hapticFeedback)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_random).setOnClickListener {
            prefs.keyRandomization = !prefs.keyRandomization
            secureKeyboard.setKeyRandomization(prefs.keyRandomization)
            secureKeyboard.setupKeyboard()
            updateStatus(randomStatus, prefs.keyRandomization)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_paste).setOnClickListener {
            prefs.pasteButton = !prefs.pasteButton
            secureKeyboard.setPasteEnabled(prefs.pasteButton)
            secureKeyboard.setupKeyboard()
            updateStatus(pasteStatus, prefs.pasteButton)
        }

        popupView.findViewById<LinearLayout>(R.id.privacy_copy).setOnClickListener {
            prefs.copyButton = !prefs.copyButton
            secureKeyboard.setCopyEnabled(prefs.copyButton)
            secureKeyboard.setupKeyboard()
            updateStatus(copyStatus, prefs.copyButton)
        }

        popup.showAtLocation(anchor, Gravity.TOP or Gravity.END, 16, 80)
    }

    private fun showTabsMenu(anchor: View) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.tabs_menu, null)

        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 8f
        popup.setBackgroundDrawable(getDrawable(R.color.surface))

        val tabsList = popupView.findViewById<RecyclerView>(R.id.tabs_list)
        tabsList.layoutManager = LinearLayoutManager(this)

        val adapter = TabAdapter(tabs.toList(), currentTabId,
            onClick = { tab ->
                currentTabId = tab.id
                webView.loadUrl(tab.url)
                popup.dismiss()
            },
            onClose = { tab ->
                tabs.remove(tab)
                if (tabs.isEmpty()) {
                    tabs.add(Tab(0, "https://duckduckgo.com", "New Tab"))
                    currentTabId = 0
                    webView.loadUrl("https://duckduckgo.com")
                }
                tabsList.adapter?.let { (it as TabAdapter).updateData(tabs.toList(), currentTabId) }
            }
        )
        tabsList.adapter = adapter

        popupView.findViewById<ImageView>(R.id.tabs_back).setOnClickListener {
            popup.dismiss()
            showMainMenu(anchor)
        }

        popupView.findViewById<ImageView>(R.id.tabs_new).setOnClickListener {
            val newId = (tabs.maxOfOrNull { it.id } ?: 0) + 1
            tabs.add(Tab(newId, "https://duckduckgo.com", "New Tab"))
            currentTabId = newId
            webView.loadUrl("https://duckduckgo.com")
            popup.dismiss()
        }

        popup.showAtLocation(anchor, Gravity.TOP or Gravity.END, 16, 80)
    }

    private fun showClipboardMenu(anchor: View) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.clipboard_menu, null)

        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 8f
        popup.setBackgroundDrawable(getDrawable(R.color.surface))

        val clipboardList = popupView.findViewById<RecyclerView>(R.id.clipboard_list)
        val emptyView = popupView.findViewById<TextView>(R.id.clipboard_empty)
        clipboardList.layoutManager = LinearLayoutManager(this)

        val entries = internalClipboard.getHistory()
        if (entries.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            clipboardList.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            clipboardList.visibility = View.VISIBLE
        }

        val adapter = ClipboardAdapter(entries,
            onCopy = { entry ->
                if (webView != null && secureKeyboard.visibility == View.VISIBLE) {
                    webView.evaluateJavascript("""
                        (function() {
                            var el = document.activeElement;
                            if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) {
                                var s = el.selectionStart || el.value.length;
                                var v = el.value || '';
                                el.value = v.substring(0, s) + '${entry.text.replace("'", "\\'")}' + v.substring(s);
                                el.setSelectionRange(s + '${entry.text.replace("'", "\\'")}'.length, s + '${entry.text.replace("'", "\\'")}'.length);
                                el.dispatchEvent(new Event('input', {bubbles: true}));
                            }
                        })();
                    """.trimIndent(), null)
                } else {
                    urlBar.text?.insert(urlBar.selectionStart, entry.text)
                }
            },
            onDelete = { entry ->
                internalClipboard.deleteEntry(entry.timestamp)
                val newEntries = internalClipboard.getHistory()
                if (newEntries.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    clipboardList.visibility = View.GONE
                }
                clipboardList.adapter?.let { (it as ClipboardAdapter).updateData(newEntries) }
            }
        )
        clipboardList.adapter = adapter

        popupView.findViewById<ImageView>(R.id.clipboard_back).setOnClickListener {
            popup.dismiss()
            showMainMenu(anchor)
        }

        popupView.findViewById<ImageView>(R.id.clipboard_clear).setOnClickListener {
            internalClipboard.clear()
            emptyView.visibility = View.VISIBLE
            clipboardList.visibility = View.GONE
            clipboardList.adapter?.let { (it as ClipboardAdapter).updateData(emptyList()) }
        }

        popupView.findViewById<LinearLayout>(R.id.copy_to_external).setOnClickListener {
            val latest = internalClipboard.paste()
            if (latest != null) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Taybeti Browser", latest)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to system clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No items in clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        popup.showAtLocation(anchor, Gravity.TOP or Gravity.END, 16, 80)
    }

    private fun showDownloadsMenu(anchor: View) {
        Toast.makeText(this, "Downloads coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showHistoryMenu(anchor: View) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.history_menu, null)

        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 8f
        popup.setBackgroundDrawable(getDrawable(R.color.surface))

        val historyList = popupView.findViewById<RecyclerView>(R.id.history_list)
        historyList.layoutManager = LinearLayoutManager(this)

        val adapter = HistoryAdapter(db.getAllHistory(),
            onClick = { entry ->
                popup.dismiss()
                webView.loadUrl(entry.url)
            },
            onDelete = { entry ->
                db.deleteHistoryEntry(entry.id)
                historyList.adapter?.let { (it as HistoryAdapter).updateData(db.getAllHistory()) }
            }
        )
        historyList.adapter = adapter

        popup.showAtLocation(anchor, Gravity.TOP or Gravity.END, 16, 80)
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

        @JavascriptInterface
        fun onTextSelected(text: String) {
            runOnUiThread {
                if (prefs.copyButton) {
                    internalClipboard.copy(text)
                    Toast.makeText(this@MainActivity, "Copied to secure clipboard", Toast.LENGTH_SHORT).show()
                }
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

    private fun setupUrlBar() {
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
                    val url = if (prefs.httpsUpgrade && !input.contains("://")) {
                        if (input.startsWith("http://")) {
                            input.replace("http://", "https://")
                        } else if (input.contains(".") && !input.contains(" ")) {
                            "https://$input"
                        } else {
                            "https://www.google.com/search?q=${input.replace(" ", "+")}"
                        }
                    } else if (!input.contains("://")) {
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

        btnBookmark.setOnClickListener {
            val currentUrl = webView.url ?: return@setOnClickListener
            val currentTitle = webView.title ?: "Bookmark"
            showBookmarkDialog(currentTitle, currentUrl)
        }

        btnNewTab.setOnClickListener {
            createNewTab()
        }
    }

    private fun createNewTab() {
        val newId = (tabs.maxOfOrNull { it.id } ?: 0) + 1
        tabs.add(Tab(newId, "https://duckduckgo.com", "New Tab"))
        currentTabId = newId
        webView.loadUrl("https://duckduckgo.com")
        updateTabsUI()
    }

    private fun showBookmarkDialog(title: String, url: String) {
        val editText = EditText(this).apply {
            setText(title)
            setTextColor(getColor(R.color.text_primary))
            setBackgroundColor(getColor(R.color.surface_variant))
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Save Bookmark")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = editText.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    db.addBookmark(newTitle, url)
                    Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTabsUI() {
        tabsContainer.visibility = if (tabs.size > 1) View.VISIBLE else View.GONE

        for (i in tabsRow.childCount - 1 downTo 1) {
            tabsRow.removeViewAt(i)
        }

        for (tab in tabs) {
            val indicator = android.widget.TextView(this).apply {
                text = tab.title.take(12).ifEmpty { "Tab" }
                textSize = 12f
                setTextColor(getColor(if (tab.id == currentTabId) R.color.accent else R.color.text_secondary))
                setPadding(16, 8, 16, 8)
                background = if (tab.id == currentTabId) {
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(getColor(R.color.surface_variant))
                        cornerRadius = 16f
                    }
                } else null
                setOnClickListener {
                    currentTabId = tab.id
                    webView.loadUrl(tab.url)
                    updateTabsUI()
                }
            }
            tabsRow.addView(indicator)
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