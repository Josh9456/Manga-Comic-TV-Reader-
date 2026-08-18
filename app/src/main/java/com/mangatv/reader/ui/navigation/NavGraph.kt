package com.mangatv.reader.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mangatv.reader.ui.components.TvNavTab
import com.mangatv.reader.ui.explorer.TvFileManagerScreen
import com.mangatv.reader.ui.library.TvLibraryScreen
import com.mangatv.reader.ui.reader.TvComicReaderScreen
import com.mangatv.reader.ui.settings.TvSettingsScreen

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Explorer : Screen("explorer")
    data object Settings : Screen("settings")
    data object Reader : Screen("reader/{filePath}") {
        fun createRoute(filePath: String): String {
            val encoded = Uri.encode(filePath)
            return "reader/$encoded"
        }
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Library.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Library.route) {
            TvLibraryScreen(
                onNavigateToReader = { path ->
                    navController.navigate(Screen.Reader.createRoute(path))
                },
                onNavigateToTab = { tab ->
                    when (tab) {
                        TvNavTab.LIBRARY -> {}
                        TvNavTab.EXPLORER -> navController.navigate(Screen.Explorer.route) {
                            launchSingleTop = true
                        }
                        TvNavTab.SETTINGS -> navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Screen.Explorer.route) {
            TvFileManagerScreen(
                onNavigateToReader = { path ->
                    navController.navigate(Screen.Reader.createRoute(path))
                },
                onNavigateToTab = { tab ->
                    when (tab) {
                        TvNavTab.LIBRARY -> navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Library.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        TvNavTab.EXPLORER -> {}
                        TvNavTab.SETTINGS -> navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            TvSettingsScreen(
                onNavigateToTab = { tab ->
                    when (tab) {
                        TvNavTab.LIBRARY -> navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Library.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        TvNavTab.EXPLORER -> navController.navigate(Screen.Explorer.route) {
                            launchSingleTop = true
                        }
                        TvNavTab.SETTINGS -> {}
                    }
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(
                navArgument("filePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val decodedPath = Uri.decode(encodedPath)
            TvComicReaderScreen(
                filePath = decodedPath,
                onBackToLibrary = {
                    navController.popBackStack()
                }
            )
        }
    }
}
