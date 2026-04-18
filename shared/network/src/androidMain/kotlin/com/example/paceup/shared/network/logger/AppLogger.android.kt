package com.example.paceup.shared.network.logger

import android.util.Log

actual object AppLogger {

    private var isDebug = false

    actual fun setDebugEnabled(enabled: Boolean) {
        isDebug = enabled
    }

    actual fun d(tag: String, message: String) {
        if (isDebug) Log.d("PaceUp", "[$tag] $message")
    }

    actual fun i(tag: String, message: String) {
        if (isDebug) Log.i("PaceUp", "[$tag] $message")
    }

    actual fun w(tag: String, message: String) {
        if (isDebug) Log.w("PaceUp", "[$tag] $message")
    }

    actual fun e(tag: String, message: String) {
        if (isDebug) Log.e("PaceUp", "[$tag] $message")
    }
}
