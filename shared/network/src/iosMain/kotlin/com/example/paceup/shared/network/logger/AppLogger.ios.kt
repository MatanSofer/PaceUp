package com.example.paceup.shared.network.logger

import platform.Foundation.NSLog

// iOS: actual implementation — verify on Mac before PR

actual object AppLogger {

    private var isDebug = false

    actual fun setDebugEnabled(enabled: Boolean) {
        isDebug = enabled
    }

    actual fun d(tag: String, message: String) {
        if (isDebug) NSLog("[PaceUp][D][%@] %@", tag, message)
    }

    actual fun i(tag: String, message: String) {
        if (isDebug) NSLog("[PaceUp][I][%@] %@", tag, message)
    }

    actual fun w(tag: String, message: String) {
        if (isDebug) NSLog("[PaceUp][W][%@] %@", tag, message)
    }

    actual fun e(tag: String, message: String) {
        if (isDebug) NSLog("[PaceUp][E][%@] %@", tag, message)
    }
}
