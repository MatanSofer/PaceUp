package com.example.paceup.shared.network.logger

/**
 * Shared logging wrapper. Call [setDebugEnabled] once on app start with the build-type flag.
 * All log output is suppressed until debug mode is enabled — nothing is emitted in release.
 *
 * Usage: `AppLogger.d("AuthRepository", "sign-in started")`
 */
expect object AppLogger {

    /** Enable or disable all log output. Pass `BuildConfig.DEBUG` from the app module. */
    fun setDebugEnabled(enabled: Boolean)

    /** Debug — fine-grained flow tracing. */
    fun d(tag: String, message: String)

    /** Info — lifecycle and significant state changes. */
    fun i(tag: String, message: String)

    /** Warning — recoverable unexpected conditions. */
    fun w(tag: String, message: String)

    /** Error — caught exceptions and failures. */
    fun e(tag: String, message: String)
}
