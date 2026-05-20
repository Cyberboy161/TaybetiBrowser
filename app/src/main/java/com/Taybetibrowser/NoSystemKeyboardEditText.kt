package com.Taybetibrowser

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

class NoSystemKeyboardEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : EditText(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        showSoftInputOnFocus = false
        setTextIsSelectable(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            requestFocus()
            hideSystemKeyboard()
            setSelection(text?.length ?: 0, text?.length ?: 0)
        }
        return super.onTouchEvent(event)
    }

    override fun onCreateInputConnection(editorInfo: android.view.inputmethod.EditorInfo): android.view.inputmethod.InputConnection? {
        return null
    }

    private fun hideSystemKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    override fun getSelectionStart(): Int = text?.length ?: 0
    override fun getSelectionEnd(): Int = text?.length ?: 0

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (selStart != selEnd && text != null) {
            setSelection(text.length, text.length)
        }
    }
}