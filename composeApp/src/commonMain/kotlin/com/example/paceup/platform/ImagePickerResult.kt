package com.example.paceup.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton bridge between the platform image picker (transparent Activity / UIImagePicker)
 * and the ViewModel. Same pattern as [StravaOAuthCodeStore].
 */
object ImagePickerResult {
    private val _pendingBytes = MutableStateFlow<ByteArray?>(null)
    val pendingBytes: StateFlow<ByteArray?> = _pendingBytes.asStateFlow()

    fun submit(bytes: ByteArray) { _pendingBytes.value = bytes }
    fun clear() { _pendingBytes.value = null }
}
