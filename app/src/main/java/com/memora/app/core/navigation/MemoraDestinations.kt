package com.memora.app.core.navigation

import com.memora.app.domain.model.AddMode

/**
 * Navigation destinations of the app.
 *
 * This file is intentionally pure Kotlin (no Android or Compose imports) so
 * route construction can be unit-tested on the JVM without an emulator.
 * Labels and icons for destinations live in the UI layer
 * (see [MemoraBottomBar]).
 */
sealed interface MemoraDestination {

    /** Route pattern as registered with the NavHost. */
    val route: String

    /** Route used when the destination is selected from the bottom bar. */
    val tabRoute: String get() = route

    // ------------------------------------------------------------------
    // Top-level destinations (bottom navigation bar)
    // ------------------------------------------------------------------

    data object Home : MemoraDestination {
        override val route: String = "home"
    }

    data object Search : MemoraDestination {
        override val route: String = "search"
    }

    data object Add : MemoraDestination {
        /** Optional nav argument selecting the initial [AddMode]. */
        const val MODE_ARG: String = "mode"

        override val route: String = "add?$MODE_ARG={$MODE_ARG}"

        override val tabRoute: String get() = routeFor(AddMode.ITEM)

        /** Route to open the Add screen in a specific mode. */
        fun routeFor(mode: AddMode): String = "add?$MODE_ARG=${mode.name}"
    }

    data object Documents : MemoraDestination {
        override val route: String = "documents"
    }

    data object More : MemoraDestination {
        override val route: String = "more"
    }

    // ------------------------------------------------------------------
    // Secondary destinations (section screens)
    // ------------------------------------------------------------------

    data object ItemDetail : MemoraDestination {
        const val ITEM_ID_ARG: String = "itemId"

        override val route: String = "item/{$ITEM_ID_ARG}"

        /** Route to open the detail/history screen of one specific item. */
        fun routeFor(itemId: String): String = "item/$itemId"
    }

    data object Objects : MemoraDestination {
        override val route: String = "objects"
    }

    data object Locations : MemoraDestination {
        override val route: String = "locations"
    }

    data object History : MemoraDestination {
        override val route: String = "history"
    }

    data object Products : MemoraDestination {
        override val route: String = "products"
    }

    data object Reminders : MemoraDestination {
        override val route: String = "reminders"
    }

    data object Backup : MemoraDestination {
        override val route: String = "backup"
    }

    data object Settings : MemoraDestination {
        override val route: String = "settings"
    }
}

/** Top-level destinations, in the order they appear in the bottom bar. */
val TopLevelDestinations: List<MemoraDestination> = listOf(
    MemoraDestination.Home,
    MemoraDestination.Search,
    MemoraDestination.Add,
    MemoraDestination.Documents,
    MemoraDestination.More,
)

/** Every destination of the app; kept for route-integrity tests. */
val AllDestinations: List<MemoraDestination> = TopLevelDestinations + listOf(
    MemoraDestination.ItemDetail,
    MemoraDestination.Objects,
    MemoraDestination.Locations,
    MemoraDestination.History,
    MemoraDestination.Products,
    MemoraDestination.Reminders,
    MemoraDestination.Backup,
    MemoraDestination.Settings,
)
