package com.example.paceup.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/** Collects one-time [flow] events and dispatches them to [onEvent]. */
@Composable
fun <T> ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    onEvent: (T) -> Unit
) {
    LaunchedEffect(flow, key1) {
        flow.collect { onEvent(it) }
    }
}
