package com.choreme.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.choreme.R
import com.choreme.feature.agenda.AgendaScreen
import com.choreme.feature.family.FamilyScreen
import com.choreme.feature.groceries.GroceriesScreen
import com.choreme.feature.today.TodayScreen

enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Today("today", R.string.nav_today, Icons.Filled.Home),
    Groceries("groceries", R.string.nav_groceries, Icons.Filled.ShoppingCart),
    Agenda("agenda", R.string.nav_agenda, Icons.Filled.DateRange),
    Family("family", R.string.nav_family, Icons.Filled.Person),
}

/** The signed-in app. [pendingJoinRequests] badges the Family tab for parents. */
@Composable
fun ChoreMeNavHost(pendingJoinRequests: Int = 0) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val badgeCount = if (destination == TopLevelDestination.Family) pendingJoinRequests else 0
                            BadgedBox(badge = { if (badgeCount > 0) Badge { Text(badgeCount.toString()) } }) {
                                Icon(destination.icon, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Today.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.Today.route) { TodayScreen() }
            composable(TopLevelDestination.Groceries.route) { GroceriesScreen() }
            composable(TopLevelDestination.Agenda.route) { AgendaScreen() }
            composable(TopLevelDestination.Family.route) { FamilyScreen() }
        }
    }
}
