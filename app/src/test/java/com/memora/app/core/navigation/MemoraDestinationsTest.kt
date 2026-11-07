package com.memora.app.core.navigation

import com.memora.app.domain.model.AddMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Route-integrity tests. Pure JVM tests: no Android framework involved.
 */
class MemoraDestinationsTest {

    @Test
    fun `top-level destinations are the five main tabs in order`() {
        assertEquals(
            listOf<MemoraDestination>(
                MemoraDestination.Home,
                MemoraDestination.Search,
                MemoraDestination.Add,
                MemoraDestination.Documents,
                MemoraDestination.More,
            ),
            TopLevelDestinations,
        )
    }

    @Test
    fun `top-level destinations use the expected route patterns`() {
        assertEquals(
            listOf("home", "search", "add?mode={mode}", "documents", "more"),
            TopLevelDestinations.map { it.route },
        )
    }

    @Test
    fun `all destination routes are unique`() {
        val routes = AllDestinations.map { it.route } +
            MemoraDestination.Add.routeFor(AddMode.ITEM) +
            MemoraDestination.Add.routeFor(AddMode.DOCUMENT)
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `add route pattern declares the mode argument`() {
        assertEquals("add?mode={mode}", MemoraDestination.Add.route)
        assertEquals("mode", MemoraDestination.Add.MODE_ARG)
    }

    @Test
    fun `add routes carry the requested mode`() {
        assertEquals("add?mode=ITEM", MemoraDestination.Add.routeFor(AddMode.ITEM))
        assertEquals("add?mode=DOCUMENT", MemoraDestination.Add.routeFor(AddMode.DOCUMENT))
    }

    @Test
    fun `bottom bar route for add opens the default mode`() {
        // The bottom bar must not reopen the last-used mode with a stale
        // argument; it always opens the plain item flow.
        assertEquals("add?mode=ITEM", MemoraDestination.Add.tabRoute)
    }

    @Test
    fun `tab routes equal plain routes for simple destinations`() {
        TopLevelDestinations
            .filter { it != MemoraDestination.Add }
            .forEach { destination ->
                assertEquals(destination.route, destination.tabRoute)
            }
    }

    @Test
    fun `every destination has a non-blank route`() {
        AllDestinations.forEach { destination ->
            assertTrue(
                "Route of $destination must not be blank",
                destination.route.isNotBlank(),
            )
        }
    }
}
