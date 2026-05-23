package com.example.paceup.platform

import android.content.Context
import android.content.Intent

actual class ImagePicker(private val context: Context) {
    actual fun launch() {
        context.startActivity(
            Intent(context, PhotoPickerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
