package com.example.paceup.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Wrapper for UI strings that originate from either a compose resource or a dynamic value. */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringRes(val resource: StringResource) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringRes -> stringResource(resource)
}
