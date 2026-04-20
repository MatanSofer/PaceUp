package com.example.paceup

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.paceup.navigation.WelcomeRoute
import com.example.paceup.navigation.appGraph

/**
 * Root composable. Hosts the NavHost for all PaceUp screens.
 * TODO(paceup): add Supabase session check in Task 1.5 to route to HomeRoute when already logged in.
 */
@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = WelcomeRoute) {
            appGraph(navController)
        }
    }
}
