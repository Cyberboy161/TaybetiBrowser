package com.Taybetibrowser

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.widget.Button
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

    override fun startActionMode(callback: ActionMode.Callback): ActionMode? {
        return null
    }

    override fun startActionModeForChild(
        originalView: View?,
        callback: ActionMode.Callback?
    ): ActionMode? {
        return null
    }

    fun clearSelection() {
        evaluateJavascript("window.getSelection().removeAllRanges();", null)
    }
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
        secureKeyboard.setDarkTheme(prefs.darkTheme)

        jsInterfaceName = if (prefs.stealthMode) "_ak${(Math.random() * 1000).toInt()}" else "AndroidKeyboard"

        if (tabs.isEmpty()) {
            tabs.add(Tab(0, "https://duckduckgo.com", "DuckDuckGo"))
            currentTabId = 0
        }

        setupWebView()
        setupUrlBar()
        setupButtons()
        setupMenu()
        applyTheme()
        updateTabsUI()
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
                updateBookmarkIcon()
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

        if (prefs.darkTheme) {
            scripts.append("""
                (function() {
                    try {
                        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
                            document.documentElement.setAttribute('data-color-scheme', 'dark');
                        }
                        var style = document.createElement('style');
                        style.textContent = 'html { background-color: #0D0D14 !important; } body { background-color: #0D0D14 !important; }';
                        document.head.appendChild(style);
                    } catch(e) {}
                })();
            """.trimIndent())
        } else {
            scripts.append("""
                (function() {
                    try {
                        document.documentElement.setAttribute('data-color-scheme', 'light');
                        var style = document.createElement('style');
                        style.textContent = 'html { background-color: #FFFFFF !important; } body { background-color: #FFFFFF !important; }';
                        document.head.appendChild(style);
                    } catch(e) {}
                })();
            """.trimIndent())
        }

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

        if (prefs.adBlockEnabled) {
            scripts.append(PreferencesManager.getAdBlockScript())
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
                var selectionPopup = null;
                var selectionMarker = null;

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

                function isSelectableText(el) {
                    if (!el) return false;
                    var style = window.getComputedStyle(el);
                    var isInput = el.tagName === 'INPUT' || el.tagName === 'TEXTAREA';
                    var isEditable = el.isContentEditable === 'true';
                    var userSelect = style.userSelect !== 'none' && style.userSelect !== 'text';
                    return (isInput || isEditable || el.tagName === 'BODY') && userSelect;
                }

                function removeSelectionUI() {
                    if (selectionPopup) {
                        selectionPopup.remove();
                        selectionPopup = null;
                    }
                    if (selectionMarker) {
                        selectionMarker.remove();
                        selectionMarker = null;
                    }
                }

                function showSelectionUI() {
                    var selection = window.getSelection();
                    if (!selection || selection.rangeCount === 0) return;
                    
                    var selectedText = selection.toString();
                    if (selectedText.length < 1) return;

                    removeSelectionUI();

                    var range = selection.getRangeAt(0);
                    var rect = range.getBoundingClientRect();
                    
                    if (!rect || rect.width === 0) return;

                    selectionMarker = document.createElement('div');
                    selectionMarker.style.cssText = 'position:fixed;pointer-events:none;z-index:999999;' +
                        'left:' + rect.left + 'px;top:' + rect.top + 'px;' +
                        'width:' + rect.width + 'px;height:' + rect.height + 'px;' +
                        'border:2px solid #00D4AA;border-radius:4px;';
                    document.body.appendChild(selectionMarker);

                    var centerX = Math.round(rect.left + rect.width / 2);
                    var popupY = Math.round(rect.top - 10);

                    window.$jsInterfaceName.showSelectionPopup(selectedText, centerX, popupY);
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

                var selectionTimeout = null;
                document.addEventListener('selectionchange', function(e) {
                    if (selectionTimeout) clearTimeout(selectionTimeout);
                    selectionTimeout = setTimeout(function() {
                        var selection = window.getSelection();
                        if (selection && selection.toString().length > 0) {
                            showSelectionUI();
                        } else {
                            removeSelectionUI();
                        }
                    }, 150);
                });

                var lastTouchEnd = 0;
                document.addEventListener('touchend', function(e) {
                    var now = Date.now();
                    if (now - lastTouchEnd > 300) {
                        setTimeout(function() {
                            var selection = window.getSelection();
                            if (selection && selection.toString().length > 0) {
                                showSelectionUI();
                            }
                        }, 200);
                    }
                    lastTouchEnd = now;
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

        val toggleDark = popupView.findViewById<TextView>(R.id.toggle_dark)
        val toggleLight = popupView.findViewById<TextView>(R.id.toggle_light)

        if (prefs.darkTheme) {
            toggleDark.isSelected = true
            toggleLight.isSelected = false
        } else {
            toggleDark.isSelected = false
            toggleLight.isSelected = true
        }

        toggleDark.setOnClickListener {
            if (!prefs.darkTheme) {
                prefs.darkTheme = true
                toggleDark.isSelected = true
                toggleLight.isSelected = false
                popup.dismiss()
                applyTheme()
            }
        }

        toggleLight.setOnClickListener {
            if (prefs.darkTheme) {
                prefs.darkTheme = false
                toggleLight.isSelected = true
                toggleDark.isSelected = false
                popup.dismiss()
                applyTheme()
            }
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
        secureKeyboard.setDarkTheme(prefs.darkTheme)
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

        @JavascriptInterface
        fun showSelectionPopup(text: String, x: Int, y: Int) {
            runOnUiThread {
                if (!prefs.copyButton) return@runOnUiThread

                val popup = PopupWindow(this@MainActivity).apply {
                    contentView = layoutInflater.inflate(R.layout.copy_popup, null)
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    height = LinearLayout.LayoutParams.WRAP_CONTENT
                    setBackgroundDrawable(getDrawable(android.R.color.transparent))
                    isOutsideTouchable = true
                    isFocusable = true
                    elevation = 12f
                }

                val copyBtn = popup.contentView.findViewById<TextView>(R.id.popup_copy)
                val cutBtn = popup.contentView.findViewById<TextView>(R.id.popup_cut)
                val deleteBtn = popup.contentView.findViewById<TextView>(R.id.popup_delete)
                val cancelBtn = popup.contentView.findViewById<TextView>(R.id.popup_cancel)

                copyBtn.setOnClickListener {
                    internalClipboard.copy(text)
                    Toast.makeText(this@MainActivity, "Copied to secure clipboard", Toast.LENGTH_SHORT).show()
                    popup.dismiss()
                    webView.clearSelection()
                }

                cutBtn.setOnClickListener {
                    internalClipboard.copy(text)
                    webView.evaluateJavascript("""
                        (function() {
                            var sel = window.getSelection();
                            if (sel.rangeCount > 0) {
                                var range = sel.getRangeAt(0);
                                range.deleteContents();
                            }
                        })();
                    """.trimIndent(), null)
                    Toast.makeText(this@MainActivity, "Cut to secure clipboard", Toast.LENGTH_SHORT).show()
                    popup.dismiss()
                }

                deleteBtn.setOnClickListener {
                    webView.evaluateJavascript("""
                        (function() {
                            var sel = window.getSelection();
                            if (sel.rangeCount > 0) {
                                var range = sel.getRangeAt(0);
                                range.deleteContents();
                            }
                        })();
                    """.trimIndent(), null)
                    Toast.makeText(this@MainActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    popup.dismiss()
                }

                cancelBtn.setOnClickListener {
                    popup.dismiss()
                }

                popup.setOnDismissListener {
                    webView.clearSelection()
                }

                popup.showAtLocation(webView, Gravity.NO_GRAVITY, x, y - 150)
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

    private fun updateBookmarkIcon() {
        val currentUrl = webView.url ?: return
        val bookmarks = db.getAllBookmarks()
        val isBookmarked = bookmarks.any { it.url.contains(currentUrl, ignoreCase = true) || currentUrl.contains(it.url, ignoreCase = true) }
        btnBookmark.setColorFilter(if (isBookmarked) getColor(R.color.accent) else getColor(R.color.text_secondary))
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
        tabsContainer.visibility = View.VISIBLE

        for (i in tabsRow.childCount - 1 downTo 1) {
            tabsRow.removeViewAt(i)
        }

        for (tab in tabs) {
            val tabLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 4, 4, 4)
                background = if (tab.id == currentTabId) {
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(getColor(R.color.surface_variant))
                        cornerRadius = 16f
                    }
                } else null

                val textView = android.widget.TextView(this@MainActivity).apply {
                    text = tab.title.take(10).ifEmpty { "Tab" }
                    textSize = 12f
                    setTextColor(getColor(if (tab.id == currentTabId) R.color.accent else R.color.text_secondary))
                    setPadding(8, 4, 8, 4)
                }
                addView(textView)

                val closeBtn = android.widget.TextView(this@MainActivity).apply {
                    text = "✕"
                    textSize = 12f
                    setTextColor(getColor(R.color.text_hint))
                    setPadding(8, 4, 8, 4)
                    setOnClickListener {
                        closeTab(tab)
                    }
                }
                addView(closeBtn)

                setOnClickListener {
                    currentTabId = tab.id
                    webView.loadUrl(tab.url)
                    updateTabsUI()
                }
            }
            tabsRow.addView(tabLayout)
        }
    }

    private fun closeTab(tab: Tab) {
        val index = tabs.indexOf(tab)
        if (index != -1) {
            tabs.removeAt(index)
            if (tabs.isEmpty()) {
                tabs.add(Tab(0, "https://duckduckgo.com", "DuckDuckGo"))
                currentTabId = 0
                webView.loadUrl("https://duckduckgo.com")
            } else if (tab.id == currentTabId) {
                val newIndex = minOf(index, tabs.size - 1)
                currentTabId = tabs[newIndex].id
                webView.loadUrl(tabs[newIndex].url)
            }
            updateTabsUI()
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