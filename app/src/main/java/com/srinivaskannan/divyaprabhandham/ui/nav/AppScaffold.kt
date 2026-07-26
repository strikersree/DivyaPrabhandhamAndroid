package com.srinivaskannan.divyaprabhandham.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.srinivaskannan.divyaprabhandham.billing.TipJar
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.sync.GoogleSyncManager
import com.srinivaskannan.divyaprabhandham.ui.browse.DivisionBrowserScreen
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.components.ResumePill
import com.srinivaskannan.divyaprabhandham.ui.desams.DesamDetailScreen
import com.srinivaskannan.divyaprabhandham.ui.desams.DesamsScreen
import com.srinivaskannan.divyaprabhandham.ui.home.HomeScreen
import com.srinivaskannan.divyaprabhandham.ui.reader.ReaderScreen
import com.srinivaskannan.divyaprabhandham.ui.saved.SavedScreen
import com.srinivaskannan.divyaprabhandham.ui.search.SearchScreen
import com.srinivaskannan.divyaprabhandham.ui.settings.AboutScreen
import com.srinivaskannan.divyaprabhandham.ui.settings.SettingsScreen
import com.srinivaskannan.divyaprabhandham.ui.settings.TipJarScreen
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository

/**
 * The navigation shell.
 *
 * `NavigationSuiteScaffold` is the reason this is one composable rather than
 * two: it picks a bottom bar on a phone, a rail on a tablet and a drawer on a
 * desktop-sized window by itself. The iOS build needed an explicit
 * phone-versus-iPad split for the same effect, with a whole separate Book tab
 * and split view for the large layout. Here the adaptive layout does it, which
 * is why there is no Book tab: on a wide screen the same five destinations
 * simply move to the side.
 */
@Composable
fun AppScaffold(
    sync: GoogleSyncManager,
    tipJar: TipJar,
    deepLink: DeepLink?,
    onDeepLinkHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    val repository = LocalRepository.current
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val openSection: (String, String?) -> Unit = { sectionId, stanzaKey ->
        navController.navigate(Routes.reader(sectionId, stanzaKey))
    }

    // Deep links arrive from the widget and the reminder notification. They are
    // handled here rather than in the activity so they land on the same
    // NavController the rest of the app uses, and so Back from a deep-linked
    // verse returns into the app instead of leaving it.
    LaunchedEffect(deepLink) {
        when (deepLink) {
            null -> Unit
            is DeepLink.Resume -> {
                appState.lastRead?.let { last ->
                    if (repository.section(last.sectionId) != null) {
                        openSection(last.sectionId, last.stanzaKey)
                    }
                }
                onDeepLinkHandled()
            }
            is DeepLink.OpenVerse -> {
                if (repository.section(deepLink.sectionId) != null) {
                    openSection(deepLink.sectionId, deepLink.stanzaKey)
                }
                onDeepLinkHandled()
            }
        }
    }

    // The navigation bar stays put everywhere, including the reader. iOS hides
    // the tab bar on a pushed view, but on Android a bottom bar that vanishes
    // on a detail screen reads as the app losing its footing — the platform
    // convention is that it persists.
    val isReader = currentRoute?.startsWith("reader/") == true

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            TopLevel.entries.forEach { destination ->
                val selected = currentRoute == destination.route
                item(
                    selected = selected,
                    onClick = { navController.switchTopLevel(destination) },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon
                            else destination.icon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(appState.ui(destination.label)) },
                )
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            onOpenDivision = { navController.navigate(Routes.division(it)) },
                            onOpenSection = openSection,
                        )
                    }

                    composable(Routes.DESAMS) {
                        DesamsScreen(
                            onOpenDesam = { navController.navigate(Routes.desam(it)) },
                        )
                    }

                    composable(Routes.SAVED) {
                        SavedScreen(onOpenSection = openSection)
                    }

                    composable(Routes.SEARCH) {
                        SearchScreen(onOpenSection = openSection)
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            sync = sync,
                            onOpenAbout = { navController.navigate(Routes.ABOUT) },
                            onOpenTipJar = { navController.navigate(Routes.TIP_JAR) },
                        )
                    }

                    composable(
                        route = Routes.DIVISION,
                        arguments = listOf(navArgument("divisionId") { type = NavType.StringType }),
                    ) { entry ->
                        val division = Division.byId(entry.arguments?.getString("divisionId"))
                        if (division == null) {
                            EmptyState(title = appState.ui(Ui.NO_RESULTS))
                        } else {
                            DivisionBrowserScreen(
                                division = division,
                                onBack = { navController.popBackStack() },
                                onOpenSection = { openSection(it, null) },
                            )
                        }
                    }

                    composable(
                        route = Routes.READER,
                        arguments = listOf(
                            navArgument("sectionId") { type = NavType.StringType },
                            navArgument("stanzaKey") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { entry ->
                        val sectionId = entry.arguments?.getString("sectionId")
                        val section = repository.section(sectionId)
                        if (section == null) {
                            EmptyState(title = appState.ui(Ui.NO_RESULTS))
                        } else {
                            ReaderScreen(
                                section = section,
                                work = repository.workContaining(section.id),
                                initialStanzaKey = entry.arguments?.getString("stanzaKey"),
                                onBack = { navController.popBackStack() },
                                onOpenSection = openSection,
                            )
                        }
                    }

                    composable(
                        route = Routes.DESAM_DETAIL,
                        arguments = listOf(navArgument("desamId") { type = NavType.StringType }),
                    ) { entry ->
                        val id = entry.arguments?.getString("desamId")
                        val desam = repository.divyaDesams.firstOrNull { it.id == id }
                        if (desam == null) {
                            EmptyState(title = appState.ui(Ui.NO_RESULTS))
                        } else {
                            DesamDetailScreen(
                                desam = desam,
                                onBack = { navController.popBackStack() },
                                onOpenSection = openSection,
                            )
                        }
                    }

                    composable(Routes.ABOUT) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }

                    composable(Routes.TIP_JAR) {
                        TipJarScreen(
                            tipJar = tipJar,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }

            // Continue Reading sits above the navigation bar, app-wide — but
            // not over the reader, where it would offer to take you where you
            // already are.
            if (!isReader) {
                ResumePill(onOpen = openSection)
            }
        }
    }
}

/** A link in from the widget or a reminder notification. */
sealed interface DeepLink {
    data object Resume : DeepLink
    data class OpenVerse(val sectionId: String, val stanzaKey: String?) : DeepLink
}
