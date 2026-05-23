package com.example.paceup.shared.auth.profile

import com.example.paceup.shared.network.result.Error

enum class ProfileError : Error {
    DISPLAY_NAME_BLANK,
    DISPLAY_NAME_TOO_SHORT,
    DISPLAY_NAME_TOO_LONG,
    AVATAR_UPLOAD_FAILED,
    SAVE_FAILED,
    NOT_AUTHENTICATED,
}
