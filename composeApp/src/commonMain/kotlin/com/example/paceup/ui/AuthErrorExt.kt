package com.example.paceup.ui

import com.example.paceup.shared.network.error.AuthError
import paceup.composeapp.generated.resources.Res
import paceup.composeapp.generated.resources.error_auth_email_in_use
import paceup.composeapp.generated.resources.error_auth_email_not_confirmed
import paceup.composeapp.generated.resources.error_auth_email_rate_limited
import paceup.composeapp.generated.resources.error_auth_invalid_credentials
import paceup.composeapp.generated.resources.error_auth_oauth_cancelled
import paceup.composeapp.generated.resources.error_auth_oauth_failed
import paceup.composeapp.generated.resources.error_auth_session_expired
import paceup.composeapp.generated.resources.error_auth_unauthorized
import paceup.composeapp.generated.resources.error_auth_unknown
import paceup.composeapp.generated.resources.error_auth_weak_password

/** Maps [AuthError] enum values to displayable [UiText] backed by string resources. */
fun AuthError.toUiText(): UiText = UiText.StringRes(
    when (this) {
        AuthError.INVALID_CREDENTIALS -> Res.string.error_auth_invalid_credentials
        AuthError.EMAIL_ALREADY_IN_USE -> Res.string.error_auth_email_in_use
        AuthError.WEAK_PASSWORD -> Res.string.error_auth_weak_password
        AuthError.EMAIL_NOT_CONFIRMED -> Res.string.error_auth_email_not_confirmed
        AuthError.EMAIL_RATE_LIMITED -> Res.string.error_auth_email_rate_limited
        AuthError.SESSION_EXPIRED -> Res.string.error_auth_session_expired
        AuthError.UNAUTHORIZED -> Res.string.error_auth_unauthorized
        AuthError.OAUTH_CANCELLED -> Res.string.error_auth_oauth_cancelled
        AuthError.OAUTH_FAILED -> Res.string.error_auth_oauth_failed
        AuthError.UNKNOWN -> Res.string.error_auth_unknown
    }
)
