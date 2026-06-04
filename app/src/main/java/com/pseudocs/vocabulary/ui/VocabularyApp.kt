package com.pseudocs.vocabulary.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.pseudocs.vocabulary.ui.screens.SettingsScreen
import com.pseudocs.vocabulary.ui.screens.VocabularyScreen
import com.pseudocs.vocabulary.ui.screens.WordDetailScreen

/**
 * Navigation routes used throughout the app.
 */
sealed class Screen(val route: String) {
    object Vocabulary : Screen("vocabulary")
    object Settings : Screen("settings")
    object WordDetail : Screen("word_detail/{wordId}") {
        fun createRoute(wordId: Long) = "word_detail/$wordId"
    }
}

/**
 * Root composable that sets up the bottom navigation and navigation graph.
 */
@Composable
fun VocabularyApp(
    wordIdToOpen: Long? = null,
    onWordIdOpened: () -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(wordIdToOpen) {
        if (wordIdToOpen != null && wordIdToOpen != -1L) {
            navController.navigate(Screen.WordDetail.createRoute(wordIdToOpen))
            onWordIdOpened()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar on top-level screens
    val showBottomBar = currentDestination?.route in listOf(
        Screen.Vocabulary.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = androidx.compose.ui.unit.Dp(0f)
                ) {
                    val items = listOf(
                        Triple(Screen.Vocabulary, Icons.Default.Book, "Vocabulary"),
                        Triple(Screen.Settings, Icons.Default.Settings, "Settings")
                    )
                    items.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Vocabulary.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Vocabulary.route) {
                VocabularyScreen(
                    onWordClick = { wordId ->
                        navController.navigate(Screen.WordDetail.createRoute(wordId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.WordDetail.route,
                arguments = listOf(navArgument("wordId") { type = NavType.LongType })
            ) { backStackEntry ->
                val wordId = backStackEntry.arguments?.getLong("wordId") ?: return@composable
                WordDetailScreen(
                    wordId = wordId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
