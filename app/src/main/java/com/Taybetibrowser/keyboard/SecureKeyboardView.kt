package com.Taybetibrowser.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.Taybetibrowser.R

class SecureKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var editText: EditText? = null
    private var webView: WebView? = null
    private var webElementId: String? = null
    private var hapticEnabled = false
    private var isShifted = false

    private val deleteHandler = Handler(Looper.getMainLooper())
    private var deleteRunnable: Runnable? = null
    private var isDeleteRepeating = false
    private var deleteSpeed = 150L
    private var isManuallyHidden = false

    enum class KeyboardMode { EN, DE, CKB, NUM }
    private var mode = KeyboardMode.EN

    private val enRows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("123","🌐","space","▼","Go")
    )

    private val deRows = listOf(
        listOf("q","w","e","r","t","z","u","i","o","p","ü"),
        listOf("a","s","d","f","g","h","j","k","l","ö","ä"),
        listOf("⇧","y","x","c","v","b","n","m","ß","⌫"),
        listOf("123","🌐","space","▼","Go")
    )

    private val ckRows = listOf(
        listOf("و","ە","ر","ت","ی","ۆ","پ","چ","ژ","ن"),
        listOf("م","ه","ێ","ل","ک","گ","س","ب","ف","ئ"),
        listOf("⇧","ش","ڕ","ق","د","ج","خ","ح","ز","⌫"),
        listOf("123","🌐","space","▼","Go")
    )

    private val numRows = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("@","#","$","%","-","+","(",")","/"),
        listOf(".",",","!","?","\"","'","↵","⌫"),
        listOf("123","🌐","space","▼","Go")
    )

    init {
        orientation = VERTICAL
        setBackgroundColor(context.getColor(R.color.keyboard_background))
        setupKeyboard()
    }

    fun setTargetEditText(editText: EditText?) {
        this.editText = editText
        this.webView = null
        this.webElementId = null
    }

    fun setWebViewTarget(webView: WebView, elementId: String) {
        this.webView = webView
        this.webElementId = elementId
        this.editText = null
    }

    fun setHapticFeedback(enabled: Boolean) {
        hapticEnabled = enabled
    }

    private fun setupKeyboard() {
        removeAllViews()

        when (mode) {
            KeyboardMode.EN -> setupRows(enRows)
            KeyboardMode.DE -> setupRows(deRows)
            KeyboardMode.CKB -> setupRows(ckRows)
            KeyboardMode.NUM -> setupRows(numRows)
        }
    }

    private fun setupRows(rows: List<List<String>>) {
        for (row in rows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
                gravity = Gravity.CENTER_HORIZONTAL
            }

            for (key in row) {
                val weight = when (key) {
                    "space" -> 2.5f
                    "Go" -> 0.8f
                    "▼" -> 0.5f
                    "123", "ABC", "EN", "🌐" -> 0.7f
                    "⌫", "↵" -> 0.7f
                    "⇧" -> 0.7f
                    else -> 1f
                }
                addKey(rowLayout, key, weight)
            }
            addView(rowLayout)
        }
    }

    private fun addKey(container: LinearLayout, key: String, weight: Float = 1f) {
        val normalColor = when (key) {
            "space", "Go", "123", "ABC", "EN", "🌐", "⌫", "↵", "⇧" -> context.getColor(R.color.keyboard_special_key)
            else -> context.getColor(R.color.keyboard_key)
        }
        val pressedColor = context.getColor(R.color.keyboard_key_pressed)

        val button = Button(context).apply {
            text = when (key) {
                "space" -> ""
                "Go" -> "Go"
                "🌐" -> when (mode) {
                    KeyboardMode.EN -> "DE"
                    KeyboardMode.DE -> "ckb"
                    KeyboardMode.CKB -> "EN"
                    else -> "EN"
                }
                else -> key
            }
            textSize = when (key) {
                "Go" -> 14f
                "🌐" -> 11f
                else -> 16f
            }
            setBackgroundColor(normalColor)
            setTextColor(context.getColor(R.color.keyboard_key_text))
            isAllCaps = false

            setOnClickListener {
                if (hapticEnabled) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                handleKeyPress(key)
            }

            setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        setBackgroundColor(pressedColor)
                        false
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        setBackgroundColor(normalColor)
                        false
                    }
                    else -> false
                }
            }

            if (key == "⌫") {
                setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            setBackgroundColor(pressedColor)
                            startDeleteRepeat()
                            true
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            setBackgroundColor(normalColor)
                            stopDeleteRepeat()
                            true
                        }
                        else -> false
                    }
                }
            }
        }

        val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
        params.setMargins(1, 1, 1, 1)
        button.layoutParams = params
        container.addView(button)
    }

    private fun startDeleteRepeat() {
        deleteSpeed = 150L
        isDeleteRepeating = true
        performDelete()

        deleteRunnable = object : Runnable {
            override fun run() {
                if (isDeleteRepeating) {
                    performDelete()
                    deleteSpeed = maxOf(30L, deleteSpeed - 15L)
                    deleteHandler.postDelayed(this, deleteSpeed)
                }
            }
        }
        deleteHandler.postDelayed(deleteRunnable!!, deleteSpeed)
    }

    private fun stopDeleteRepeat() {
        isDeleteRepeating = false
        deleteRunnable?.let { deleteHandler.removeCallbacks(it) }
        deleteRunnable = null
    }

    private fun performDelete() {
        if (webView != null && webElementId != null) {
            webView?.evaluateJavascript(
                "var el = document.getElementById('$webElementId'); if(el){var s=el.selectionStart;if(s>0){el.value=el.value.substring(0,s-1)+el.value.substring(s);el.setSelectionRange(s-1,s-1);el.dispatchEvent(new Event('input',{bubbles:true}));}}",
                null
            )
        } else {
            editText?.text?.let { editable ->
                if (editable.isNotEmpty()) {
                    val pos = editable.length
                    editable.delete(pos - 1, pos)
                }
            }
        }
    }

    private fun handleKeyPress(key: String) {
        when (key) {
            "⇧" -> {
                isShifted = !isShifted
                setupKeyboard()
            }
            "123" -> {
                mode = KeyboardMode.NUM
                isShifted = false
                setupKeyboard()
            }
            "ABC" -> {
                mode = KeyboardMode.EN
                isShifted = false
                setupKeyboard()
            }
            "🌐" -> {
                mode = when (mode) {
                    KeyboardMode.EN -> KeyboardMode.DE
                    KeyboardMode.DE -> KeyboardMode.CKB
                    KeyboardMode.CKB -> KeyboardMode.EN
                    else -> KeyboardMode.EN
                }
                isShifted = false
                setupKeyboard()
            }
            "EN" -> {
                mode = KeyboardMode.EN
                isShifted = false
                setupKeyboard()
            }
            "⌫" -> {
                if (!isDeleteRepeating) {
                    performDelete()
                }
            }
            "Go" -> {
                if (webView != null && webElementId != null) {
                    webView?.evaluateJavascript(
                        """
                        (function() {
                            var el = document.getElementById('$webElementId');
                            if (!el) el = document.activeElement;
                            if (el) {
                                el.blur();
                                el.form?.submit();
                            }
                        })();
                        """.trimIndent(),
                        null
                    )
                } else {
                    editText?.let { et ->
                        et.clearFocus()
                        performEditorAction(et, android.view.inputmethod.EditorInfo.IME_ACTION_GO)
                    }
                }
            }
            "↵" -> {
                if (webView != null && webElementId != null) {
                    webView?.evaluateJavascript(
                        """
                        (function() {
                            var el = document.getElementById('$webElementId');
                            if (!el) el = document.activeElement;
                            if (el) {
                                el.blur();
                            }
                        })();
                        """.trimIndent(),
                        null
                    )
                } else {
                    editText?.let { et ->
                        performEditorAction(et, android.view.inputmethod.EditorInfo.IME_ACTION_GO)
                    }
                }
            }
            "▼" -> {
                hide()
                visibility = View.GONE
            }
            "space" -> {
                if (webView != null && webElementId != null) {
                    webView?.evaluateJavascript(
                        """
                        (function() {
                            var el = document.getElementById('$webElementId');
                            if (!el) el = document.activeElement;
                            if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) {
                                var s = el.selectionStart || el.value.length;
                                var v = el.value || '';
                                el.value = v.substring(0, s) + ' ' + v.substring(s);
                                el.setSelectionRange(s + 1, s + 1);
                                el.dispatchEvent(new Event('input', {bubbles: true}));
                            }
                        })();
                        """.trimIndent(),
                        null
                    )
                } else {
                    editText?.text?.insert(editText?.selectionStart ?: 0, " ")
                }
            }
            else -> {
                val charToInsert = if (isShifted && key.length == 1) key.uppercase() else key

                if (webView != null && webElementId != null) {
                    val escaped = charToInsert.replace("'", "\\'").replace("\\", "\\\\")
                    webView?.evaluateJavascript(
                        """
                        (function() {
                            var el = document.getElementById('$webElementId');
                            if (!el) el = document.activeElement;
                            if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) {
                                var s = el.selectionStart || el.value.length;
                                var v = el.value || '';
                                el.value = v.substring(0, s) + '$escaped' + v.substring(s);
                                el.setSelectionRange(s + 1, s + 1);
                                el.dispatchEvent(new Event('input', {bubbles: true}));
                            }
                        })();
                        """.trimIndent(),
                        null
                    )
                } else {
                    editText?.text?.insert(editText?.selectionStart ?: 0, charToInsert)
                }

                if (isShifted && key.length == 1) {
                    isShifted = false
                }
            }
        }
    }

    private fun performEditorAction(editText: EditText, actionId: Int) {
        try {
            val method = EditText::class.java.getMethod("onEditorAction", Int::class.java)
            method.invoke(editText, actionId)
        } catch (e: Exception) {
            editText.clearFocus()
        }
    }

    fun show() {
        visibility = View.VISIBLE
        isManuallyHidden = false
    }

    fun hide() {
        visibility = View.GONE
        isManuallyHidden = true
    }

    fun isHidden(): Boolean = isManuallyHidden

    fun clearTarget() {
        editText = null
        webView = null
        webElementId = null
    }
}