package com.marcm.cadencia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marcm.cadencia.ui.all.AllTasksScreen
import com.marcm.cadencia.ui.detail.TaskDetailScreen
import com.marcm.cadencia.ui.edit.EditTaskScreen
import com.marcm.cadencia.ui.lock.PinSetupScreen
import com.marcm.cadencia.ui.onboarding.OnboardingScreen
import com.marcm.cadencia.ui.plan.PlanScreen
import com.marcm.cadencia.ui.settings.SettingsScreen
import com.marcm.cadencia.ui.streaks.StreaksScreen
import com.marcm.cadencia.ui.theme.Accent
import com.marcm.cadencia.ui.theme.AccentInk
import com.marcm.cadencia.ui.theme.AccentSoft
import com.marcm.cadencia.ui.theme.DarkNavBar
import com.marcm.cadencia.ui.today.TodayScreen

private data class TopDest(val route: String, val label: String, val icon: ImageVector)

private val topDestinations = listOf(
    TopDest(Routes.TODAY, "Hoy", Icons.Filled.Today),
    TopDest(Routes.ALL, "Todas", Icons.AutoMirrored.Filled.FormatListBulleted),
    TopDest(Routes.PLAN, "Plan", Icons.Filled.CalendarMonth),
    TopDest(Routes.STREAKS, "Rachas", Icons.Filled.LocalFireDepartment),
    TopDest(Routes.SETTINGS, "Ajustes", Icons.Filled.Tune)
)

@Composable
fun KuseNavHost(startDestination: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBars = currentRoute in topDestinations.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBars) {
                NavigationBar(containerColor = DarkNavBar) {
                    topDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, dest.label) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                indicatorColor = AccentSoft,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Routes.TODAY) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.edit()) },
                    containerColor = Accent,
                    contentColor = AccentInk
                ) {
                    Icon(Icons.Filled.Add, "Añadir tarea")
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = startDestination) {

            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    navController.navigate(Routes.TODAY) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                })
            }

            composable(Routes.TODAY) {
                TodayScreen(
                    contentPadding = innerPadding,
                    onTaskClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }

            composable(Routes.ALL) {
                AllTasksScreen(
                    contentPadding = innerPadding,
                    onTaskClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }

            composable(Routes.PLAN) {
                PlanScreen(
                    contentPadding = innerPadding,
                    onTaskClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }

            composable(Routes.STREAKS) {
                StreaksScreen(
                    contentPadding = innerPadding,
                    onTaskClick = { id -> navController.navigate(Routes.detail(id)) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    contentPadding = innerPadding,
                    onConfigurarPin = { navController.navigate(Routes.PIN_SETUP) }
                )
            }

            composable(Routes.PIN_SETUP) {
                PinSetupScreen(
                    onListo = { navController.popBackStack() },
                    onCancelar = { navController.popBackStack() }
                )
            }

            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument(Routes.ARG_TASK_ID) { type = NavType.StringType })
            ) {
                TaskDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.edit(id)) }
                )
            }

            composable(
                Routes.EDIT,
                arguments = listOf(navArgument(Routes.ARG_TASK_ID) { type = NavType.StringType })
            ) {
                EditTaskScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}
