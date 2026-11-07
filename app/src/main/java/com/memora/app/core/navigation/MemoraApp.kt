package com.memora.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.memora.app.R
import com.memora.app.app.AppContainer
import com.memora.app.app.MemoraApplication
import com.memora.app.app.MemoraViewModelFactory
import com.memora.app.core.common.PlaceholderScreen
import com.memora.app.domain.model.AddMode
import com.memora.app.feature.add.AddScreen
import com.memora.app.feature.detail.ItemDetailScreen
import com.memora.app.feature.documents.DocumentsScreen
import com.memora.app.feature.home.HomeScreen
import com.memora.app.feature.more.MoreScreen
import com.memora.app.feature.search.SearchScreen

/**
 * Root composable of Memora: a Scaffold with the five-item bottom navigation
 * bar and the navigation graph.
 */
@Composable
fun MemoraApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val app = LocalContext.current.applicationContext as MemoraApplication
    val container = app.container

    Scaffold(
        bottomBar = {
            MemoraBottomBar(
                currentRoute = currentRoute,
                onNavigateToTab = { destination -> navController.navigateToTopLevel(destination) },
            )
        },
    ) { innerPadding ->
        MemoraNavHost(
            navController = navController,
            container = container,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Bottom navigation bar with the five top-level destinations.
 * Selection follows the current NavHost destination.
 */
@Composable
private fun MemoraBottomBar(
    currentRoute: String?,
    onNavigateToTab: (MemoraDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigateToTab(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelRes())) },
            )
        }
    }
}

/** Standard tab behaviour: one copy of each tab on the back stack. */
private fun NavHostController.navigateToTopLevel(destination: MemoraDestination) {
    navigate(destination.tabRoute) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MemoraNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = MemoraDestination.Home.route,
        modifier = modifier,
    ) {
        composable(MemoraDestination.Home.route) {
            HomeScreen(
                onOpenItem = { itemId ->
                    navController.navigate(MemoraDestination.ItemDetail.routeFor(itemId))
                },
                viewModel = viewModel(factory = MemoraViewModelFactory(container)),
            )
        }
        composable(MemoraDestination.Search.route) {
            SearchScreen(
                onOpenItem = { itemId ->
                    navController.navigate(MemoraDestination.ItemDetail.routeFor(itemId))
                },
                viewModel = viewModel(factory = MemoraViewModelFactory(container)),
            )
        }
        composable(
            route = MemoraDestination.Add.route,
            arguments = listOf(
                navArgument(MemoraDestination.Add.MODE_ARG) {
                    type = NavType.StringType
                    defaultValue = AddMode.ITEM.name
                },
            ),
        ) { entry ->
            val mode = entry.arguments?.getString(MemoraDestination.Add.MODE_ARG)
                ?.let { raw -> AddMode.entries.firstOrNull { it.name == raw } }
                ?: AddMode.ITEM
            AddScreen(
                viewModel = viewModel(factory = MemoraViewModelFactory(container, addMode = mode)),
                onSaved = { navController.popBackStack() },
            )
        }
        composable(MemoraDestination.Documents.route) {
            DocumentsScreen()
        }
        composable(MemoraDestination.More.route) {
            MoreScreen(
                onOpenSection = { route -> navController.navigate(route) },
            )
        }

        // Item detail: current location highlighted, past addresses below.
        composable(
            route = MemoraDestination.ItemDetail.route,
            arguments = listOf(
                navArgument(MemoraDestination.ItemDetail.ITEM_ID_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { entry ->
            val itemId = entry.arguments
                ?.getString(MemoraDestination.ItemDetail.ITEM_ID_ARG)
                .orEmpty()
            ItemDetailScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel(
                    factory = MemoraViewModelFactory(container, itemId = itemId),
                ),
            )
        }

        // Section screens. Each replaces its placeholder composable once the
        // feature lands in a later phase.
        composable(MemoraDestination.Objects.route) {
            PlaceholderScreen(
                title = stringResource(R.string.objects_title),
                description = stringResource(R.string.objects_pending),
            )
        }
        composable(MemoraDestination.Locations.route) {
            PlaceholderScreen(
                title = stringResource(R.string.locations_title),
                description = stringResource(R.string.locations_pending),
            )
        }
        composable(MemoraDestination.History.route) {
            PlaceholderScreen(
                title = stringResource(R.string.history_title),
                description = stringResource(R.string.history_pending),
            )
        }
        composable(MemoraDestination.Products.route) {
            PlaceholderScreen(
                title = stringResource(R.string.products_title),
                description = stringResource(R.string.products_pending),
            )
        }
        composable(MemoraDestination.Reminders.route) {
            PlaceholderScreen(
                title = stringResource(R.string.reminders_title),
                description = stringResource(R.string.reminders_pending),
            )
        }
        composable(MemoraDestination.Backup.route) {
            PlaceholderScreen(
                title = stringResource(R.string.backup_title),
                description = stringResource(R.string.backup_pending),
            )
        }
        composable(MemoraDestination.Settings.route) {
            PlaceholderScreen(
                title = stringResource(R.string.settings_title),
                description = stringResource(R.string.settings_pending),
            )
        }
    }
}

// ----------------------------------------------------------------------
// Destination presentation (labels + icons live in the UI layer)
// ----------------------------------------------------------------------

@StringRes
private fun MemoraDestination.labelRes(): Int = when (this) {
    MemoraDestination.Home -> R.string.nav_home
    MemoraDestination.Search -> R.string.nav_search
    MemoraDestination.Add -> R.string.nav_add
    MemoraDestination.Documents -> R.string.nav_documents
    MemoraDestination.More -> R.string.nav_more
    // Secondary destinations are not in the bottom bar; they use their own
    // screen titles. This branch only exists to keep the mapping total.
    else -> R.string.app_name
}

private fun MemoraDestination.icon(): ImageVector = when (this) {
    MemoraDestination.Home -> Icons.Filled.Home
    MemoraDestination.Search -> Icons.Filled.Search
    MemoraDestination.Add -> Icons.Filled.Add
    MemoraDestination.Documents -> Icons.Filled.List
    MemoraDestination.More -> Icons.Filled.MoreVert
    else -> Icons.Filled.List
}
