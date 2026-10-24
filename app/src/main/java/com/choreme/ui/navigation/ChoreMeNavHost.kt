package com.choreme.ui.navigation

import com.choreme.R
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.choreme.feature.agenda.AGENDA_ROUTE
import com.choreme.feature.agenda.agendaScreen
import com.choreme.feature.chores.TODAY_ROUTE
import com.choreme.feature.chores.todayScreen
import com.choreme.feature.family.FAMILY_ROUTE
import com.choreme.feature.family.familyScreen
import com.choreme.feature.groceries.GROCERIES_ROUTE
import com.choreme.feature.groceries.groceriesScreen

enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Today(TODAY_ROUTE, R.string.nav_today, Icons.Filled.Home),
    Groceries(GROCERIES_ROUTE, R.string.nav_groceries, Icons.Filled.ShoppingCart),
    Agenda(AGENDA_ROUTE, R.string.nav_agenda, Icons.Filled.DateRange),
    Family(FAMILY_ROUTE, R.string.nav_family, Icons.Filled.Person),
}

/** The signed-in app. The counts badge the Family and Today tabs for parents. */
@Composable
fun ChoreMeNavHost(pendingJoinRequests: Int = 0, pendingApprovals: Int = 0) {
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
                        onClick = { navController.navigateToTab(destination.route) },
                        icon = {
                            val badgeCount = when (destination) {
                                TopLevelDestination.Family -> pendingJoinRequests
                                TopLevelDestination.Today -> pendingApprovals
                                else -> 0
                            }
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
            todayScreen(onOpenGroceries = { navController.navigateToTab(TopLevelDestination.Groceries.route) })
            groceriesScreen()
            agendaScreen()
            familyScreen()
        }
    }
}

/** Switches tabs the way the bottom bar does, keeping each tab's state. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
