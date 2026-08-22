package com.srinivaskannan.divyaprabhandham.ui.nav

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.srinivaskannan.divyaprabhandham.data.Ui

/**
 * Every place the app can be.
 *
 * Routes are plain strings rather than type-safe navigation destinations. That
 * is a deliberate choice: the widget and the reminder notification both deep
 * link in by URI, and a string route is the same thing the deep link already
 * is, so there is one representation instead of two that have to agree.
 */
object Routes {
    const val HOME = "home"
    const val DESAMS = "desams"
    const val COLLECTIONS = "collections"
    const val FAVOURITES = "favourites"
    const val SEARCH = "search"
    const val SETTINGS = "settings"

    const val DIVISION = "division/{divisionId}"
    const val READER = "reader/{sectionId}?stanzaKey={stanzaKey}"
    const val DESAM_DETAIL = "desam/{desamId}"
    const val COLLECTION_DETAIL = "collection/{collectionId}"
    const val COLLECTION_READ = "collection/{collectionId}/read"
    const val ABOUT = "about"
    const val TIP_JAR = "tipjar"

    fun division(divisionId: String) = "division/$divisionId"

    fun desam(desamId: String) = "desam/$desamId"

    fun collectionDetail(collectionId: String) = "collection/$collectionId"

    fun collectionRead(collectionId: String) = "collection/$collectionId/read"

    /**
     * The stanza key is encoded because it contains a '#', which a nav route
     * would otherwise read as a fragment and silently drop — taking
     * jump-to-pasuram and resume-where-you-left-off with it.
     */
    fun reader(sectionId: String, stanzaKey: String? = null): String {
        val base = "reader/${Uri.encode(sectionId)}"
        return if (stanzaKey == null) base
        else "$base?stanzaKey=${Uri.encode(stanzaKey)}"
    }
}

/** The top-level destinations shown in the navigation bar or rail. */
enum class TopLevel(
    val route: String,
    val label: Ui,
    val selectedIcon: ImageVector,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, Ui.HOME, Icons.Filled.Home, Icons.Outlined.Home),
    DESAMS(Routes.DESAMS, Ui.DIVYA_DESAMS_TAB, Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    COLLECTIONS(Routes.COLLECTIONS, Ui.COLLECTIONS, Icons.Filled.Collections, Icons.Outlined.Collections),
    SEARCH(Routes.SEARCH, Ui.SEARCH, Icons.Filled.Search, Icons.Outlined.Search),
    SETTINGS(Routes.SETTINGS, Ui.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings),
}

/**
 * Switches top-level destination the way a navigation bar should: pop back to
 * the graph's start, keep each tab's own state, and never stack duplicates.
 */
/**
 * Switches to a top-level tab from the navigation bar.
 *
 * All screens live in one flat graph whose start destination is HOME. Tapping a
 * tab pops back to HOME and opens the tab, and the reported bug was that Home
 * itself did nothing when the current screen was pushed from Home (a division
 * browser or reader): the naive popUpTo(start, saveState) + restoreState=true
 * pattern would pop to Home and then restore the saved stack, landing back on
 * the detail screen.
 *
 * The fix is restoreState = (destination != HOME): every tab restores its own
 * saved stack except Home, which always lands on a fresh top. See the case
 * analysis in the commit — from a detail screen, from another tab, and after
 * saving a Home sub-stack, all four paths land on Home.
 */
fun NavController.switchTopLevel(destination: TopLevel) {
    val startId = graph.findStartDestination().id
    navigate(destination.route) {
        popUpTo(startId) {
            // Home is the root: land on it. Other tabs sit alongside it and
            // keep their own state across switches.
            inclusive = false
            saveState = destination != TopLevel.HOME
        }
        launchSingleTop = true
        restoreState = destination != TopLevel.HOME
    }
}
