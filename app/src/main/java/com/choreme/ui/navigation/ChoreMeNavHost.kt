package com.choreme.ui.navigation

import com.choreme.R
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import com.choreme.core.ui.R as CoreUiR
import com.choreme.core.data.WriteFailures
import com.choreme.core.data.WriteFailure
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.choreme.feature.chores.TODAY_ROUTE
import com.choreme.feature.chores.todayScreen
import com.choreme.feature.family.FAMILY_ROUTE
import com.choreme.feature.home.HOME_ROUTE
import com.choreme.feature.home.homeScreen
import com.choreme.feature.family.familyScreen
import com.choreme.feature.groceries.GROCERIES_ROUTE
import com.choreme.feature.groceries.groceriesScreen
import com.choreme.feature.map.MAP_ROUTE
import com.choreme.feature.map.mapScreen
import com.choreme.feature.chores.ADD_CHORE_KEY
import com.choreme.feature.update.UpdateBanner

enum class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int = 0,
) {
    Home(HOME_ROUTE, R.string.nav_home, Icons.Filled.Home),
    Today(TODAY_ROUTE, R.string.nav_today, Icons.Filled.CheckCircle),
    Groceries(GROCERIES_ROUTE, R.string.nav_groceries, Icons.Filled.ShoppingCart),
    Places(MAP_ROUTE, R.string.nav_map, Icons.Filled.Place),
    Family(FAMILY_ROUTE, R.string.nav_family, Icons.Filled.Person),
}

/**
 * The signed-in app. Counts on the tabs: chores that need you, groceries to buy, and (for
 * parents) people waiting to join. Groceries' count is grey: a list, not a to-do.
 */
@Composable
fun ChoreMeNavHost(pendingJoinRequests: Int = 0, choresBadge: Int = 0, groceriesToBuy: Int = 0) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // A save the family's database refused would otherwise vanish without a word.
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        WriteFailures.events.collect { failure ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(
                context.getString(if (failure == WriteFailure.REFUSED) CoreUiR.string.error_save_refused else CoreUiR.string.error_save_failed),
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
        }
    }

    Scaffold(
        topBar = { UpdateBanner() },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateToTab(destination.route) },
                        icon = {
                            val badgeCount = when (destination) {
                                TopLevelDestination.Family -> pendingJoinRequests
                                TopLevelDestination.Today -> choresBadge
                                TopLevelDestination.Groceries -> groceriesToBuy
                                else -> 0
                            }
                            val quiet = destination == TopLevelDestination.Groceries
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge(
                                            containerColor = if (quiet) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.error,
                                            contentColor = if (quiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onError,
                                        ) { Text(if (badgeCount > MAX_BADGE) "$MAX_BADGE+" else badgeCount.toString()) }
                                    }
                                },
                            ) {
                                if (destination.icon != null) {
                                    Icon(destination.icon, contentDescription = null)
                                } else {
                                    Icon(painterResource(destination.iconRes), contentDescription = null)
                                }
                            }
                        },
                        label = { Text(stringResource(destination.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            // The selected tab: ink on a yellow rounded indicator.
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            homeScreen(
                onOpenChores = { navController.navigateToTab(TopLevelDestination.Today.route) },
                onAddChore = {
                    navController.navigateToTab(TopLevelDestination.Today.route)
                    // Tells the Chores tab to open its "something needs doing" sheet.
                    runCatching { navController.getBackStackEntry(TopLevelDestination.Today.route).savedStateHandle[ADD_CHORE_KEY] = true }
                },
                onOpenGroceries = { navController.navigateToTab(TopLevelDestination.Groceries.route) },
            )
            todayScreen(onOpenGroceries = { navController.navigateToTab(TopLevelDestination.Groceries.route) })
            groceriesScreen()
            mapScreen()
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

private const val MAX_BADGE = 99
