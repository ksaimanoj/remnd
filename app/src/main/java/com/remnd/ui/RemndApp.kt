package com.remnd.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.remnd.ui.screens.AddEditReminderScreen
import com.remnd.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
    const val ADD_REMINDER = "add_reminder"
    const val EDIT_REMINDER = "edit_reminder/{reminderId}"

    fun editReminder(id: Long) = "edit_reminder/$id"
}

@Composable
fun RemndApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddReminder = { navController.navigate(Routes.ADD_REMINDER) },
                onEditReminder = { id -> navController.navigate(Routes.editReminder(id)) }
            )
        }
        composable(Routes.ADD_REMINDER) {
            AddEditReminderScreen(
                reminderId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EDIT_REMINDER,
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong("reminderId")
            AddEditReminderScreen(
                reminderId = reminderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
