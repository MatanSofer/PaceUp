package com.example.paceup.platform

/**
 * Launches the platform image picker (gallery).
 * Result is delivered via [ImagePickerResult.pendingBytes].
 * iOS: verified on Mac before PR.
 */
expect class ImagePicker {
    fun launch()
}
