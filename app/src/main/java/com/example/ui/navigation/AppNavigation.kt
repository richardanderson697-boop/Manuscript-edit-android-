package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CharacterTrackerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorProofreadScreen
import com.example.ui.screens.ExportShareScreen
import com.example.ui.viewmodel.ManuscriptViewModel

@Composable
fun AppNavigation(
    viewModel: ManuscriptViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val context = LocalContext.current
    val notification by viewModel.notification.collectAsState()

    LaunchedEffect(notification) {
        notification?.let {
            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            viewModel.dismissNotification()
        }
    }

    val screens = listOf(
        Screen.Dashboard,
        Screen.Editor,
        Screen.Characters,
        Screen.Export
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF161322),
                contentColor = Color(0xFFE2B563),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF12101C),
                            selectedTextColor = Color(0xFFE2B563),
                            indicatorColor = Color(0xFFE2B563),
                            unselectedIconColor = Color(0xFF8E88A8),
                            unselectedTextColor = Color(0xFF8E88A8)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        containerColor = Color(0xFF12101C),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = {
                        navController.navigate(Screen.Editor.route)
                    }
                )
            }

            composable(Screen.Editor.route) {
                EditorProofreadScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.Characters.route) {
                CharacterTrackerScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.Export.route) {
                ExportShareScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
