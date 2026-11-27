package com.srinivaskannan.divyaprabhandham.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.srinivaskannan.divyaprabhandham.billing.TipJar
import com.srinivaskannan.divyaprabhandham.data.Division
import com.srinivaskannan.divyaprabhandham.media.AmazonRemote
import com.srinivaskannan.divyaprabhandham.media.LocalAmazonRemote
import com.srinivaskannan.divyaprabhandham.media.LocalRecitationPosition
import com.srinivaskannan.divyaprabhandham.media.rememberRecitationPosition
import com.srinivaskannan.divyaprabhandham.sync.GoogleSyncManager
import com.srinivaskannan.divyaprabhandham.ui.browse.DivisionBrowserScreen
import com.srinivaskannan.divyaprabhandham.ui.components.EmptyState
import com.srinivaskannan.divyaprabhandham.ui.components.MusicPill
import com.srinivaskannan.divyaprabhandham.ui.components.ResumePill
import com.srinivaskannan.divyaprabhandham.ui.desams.DesamDetailScreen
import com.srinivaskannan.divyaprabhandham.ui.desams.DesamsScreen
import com.srinivaskannan.divyaprabhandham.ui.home.HomeScreen
import com.srinivaskannan.divyaprabhandham.ui.reader.ReaderScreen
import com.srinivaskannan.divyaprabhandham.ui.collections.CollectionDetailScreen
import com.srinivaskannan.divyaprabhandham.ui.collections.CollectionReaderScreen
import com.srinivaskannan.divyaprabhandham.ui.collections.CollectionsScreen
import com.srinivaskannan.divyaprabhandham.ui.favourites.FavouritesScreen
import com.srinivaskannan.divyaprabhandham.ui.search.SearchScreen
import com.srinivaskannan.divyaprabhandham.ui.settings.AboutScreen
import com.srinivaskannan.divyaprabhandham.ui.settings.SettingsScreen
import com.srinivaskannan.divyaprabhandham.ui.settings.TipJarScreen
import com.srinivaskannan.divyaprabhandham.data.Ui
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalAppState
import com.srinivaskannan.divyaprabhandham.ui.theme.LocalRepository

/**
 * The app's screens, with the remote control for Amazon Music that the
 * recitation buttons and the Now Playing pill share.
 *
 * What can be seen of Amazon Music changes while this app is in the
 * background — the grant in system Settings, playback started over there —
 * so the remote re-reads it on every return.
 */
@Composable
fun AppScaffold(
    sync: GoogleSyncManager,
    tipJar: TipJar,
    deepLink: DeepLink?,
    onDeepLinkHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val remote = remember { AmazonRemote(context.applicationContext) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) remote.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            remote.release()
        }
    }
    CompositionLocalProvider(LocalAmazonRemote provides remote) {
        NavigationShell(sync, tipJar, deepLink, onDeepLinkHandled, modifier)
    }
}

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
private fun NavigationShell(
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
            is DeepLink.TipJar -> {
                navController.navigate(Routes.TIP_JAR)
                onDeepLinkHandled()
            }
        }
    }

    // The navigation bar (and, on a tablet, the rail) stays on the reader,
    // as the tab bar does on iOS, so another tab is one tap away rather than
    // a walk back through every section read. It goes only in fullscreen,
    // which is what fullscreen is for: the whole screen for the text, the
    // reason the reader once hid the bar outright. NavigationSuiteScaffoldState
    // is the library's own mechanism for this (animated, and it consumes the
    // bar's window insets while hidden so the reader reclaims that space
    // rather than drawing over an invisible bar), unlike switching
    // `layoutType` to `.None`, which would leave the insets reserved.
    val isReader = currentRoute?.startsWith("reader/") == true
    val barHidden = isReader && appState.readerFullscreen
    // The Ask tab is a chat: its input bar sits exactly where the Continue
    // Reading pill would, so the pill is hidden there too — both to avoid the
    // collision and because "continue reading" is out of place mid-conversation.
    val isAsk = currentRoute == Routes.SEARCH

    // Null unless Amazon Music has one of this app's recitations loaded.
    val reciting = repository.amazonTrack(LocalAmazonRemote.current.shownTrack)
    // How far it has got, decade and pasuram, where the recording is timed:
    // for the pill's caption, the section list's play buttons and the
    // reader's Follow recitation.
    val recitationPosition by rememberRecitationPosition(reciting)

    val navSuiteState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(barHidden) {
        if (barHidden) navSuiteState.hide() else navSuiteState.show()
    }

    CompositionLocalProvider(LocalRecitationPosition provides recitationPosition) {
    NavigationSuiteScaffold(
        modifier = modifier,
        state = navSuiteState,
        navigationSuiteItems = {
            TopLevel.entries.forEach { destination ->
                // A detail screen pushed from a tab should keep that tab lit.
                // Division and Reader are reached from Home, Desam from Desams;
                // map those back so the highlight does not blank out on a
                // pushed screen (which also made Home look inactive there).
                val ownerRoute = when {
                    currentRoute == null -> Routes.HOME
                    currentRoute.startsWith("division/") ||
                        currentRoute.startsWith("reader/") -> Routes.HOME
                    currentRoute.startsWith("desam/") -> Routes.DESAMS
                    currentRoute == Routes.ABOUT ||
                        currentRoute == Routes.TIP_JAR -> Routes.SETTINGS
                    else -> currentRoute
                }
                val selected = ownerRoute == destination.route
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
                    label = {
                        Text(
                            appState.ui(destination.label),
                            // Long Tamil labels (e.g. "திவ்ய தேசங்கள்") wrapped
                            // to two lines and unbalanced the bar. Keep every
                            // label on one line; the nav item ellipsizes rather
                            // than wraps if it truly cannot fit.
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        },
    ) {
        // The IFrame player, parked offscreen. It must be attached to the
        // window to play, but it is a 1px sliver behind everything else.
        Column(Modifier.fillMaxSize()) {
            // In fullscreen the pill is the bottom of the screen, there being
            // no navigation bar under it, so it takes the system bar's inset
            // and the reader above must not leave room for it a second time.
            // Otherwise it sits on top of the navigation bar, as everywhere.
            val pillAtBottom = barHidden && reciting != null
            Box(
                Modifier
                    .weight(1f)
                    .then(
                        if (pillAtBottom) Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                        else Modifier,
                    ),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            tipJar = tipJar,
                            onOpenDivision = { navController.navigate(Routes.division(it)) },
                            onOpenSection = openSection,
                            onOpenFavourites = { navController.navigate(Routes.FAVOURITES) },
                            onOpenCollection = { navController.navigate(Routes.collectionRead(it)) },
                        )
                    }

                    composable(Routes.DESAMS) {
                        DesamsScreen(
                            onOpenDesam = { navController.navigate(Routes.desam(it)) },
                        )
                    }

                    composable(Routes.COLLECTIONS) {
                        CollectionsScreen(
                            onOpenCollection = { navController.navigate(Routes.collectionRead(it)) },
                        )
                    }

                    composable(Routes.FAVOURITES) {
                        FavouritesScreen(
                            onOpenSection = openSection,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SEARCH) {
                        SearchScreen(tipJar = tipJar, onOpenSection = openSection)
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            sync = sync,
                            tipJar = tipJar,
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
                            )
                        }
                    }

                    composable(
                        route = Routes.COLLECTION_DETAIL,
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
                    ) { entry ->
                        val id = entry.arguments?.getString("collectionId")
                        if (id == null) {
                            EmptyState(title = appState.ui(Ui.NO_RESULTS))
                        } else {
                            CollectionDetailScreen(
                                collectionId = id,
                                onBack = { navController.popBackStack() },
                                onOpenSection = openSection,
                            )
                        }
                    }

                    composable(
                        route = Routes.COLLECTION_READ,
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
                    ) { entry ->
                        val id = entry.arguments?.getString("collectionId")
                        if (id == null) {
                            EmptyState(title = appState.ui(Ui.NO_RESULTS))
                        } else {
                            CollectionReaderScreen(
                                collectionId = id,
                                onBack = { navController.popBackStack() },
                                onManage = { navController.navigate(Routes.collectionDetail(id)) },
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

            // One slot above the nav bar, app-wide, as on iOS: the Now
            // Playing pill while Amazon Music is reciting, Continue Reading
            // otherwise. The Ask tab's input bar has that spot, so neither
            // shows there; and Continue Reading stays off the reader, where
            // it would offer to open the current screen — but the music pill
            // is shown there, reading along being what the recitation is for.
            if (!isAsk) {
                MusicPill(
                    track = reciting,
                    // Fullscreen reading gets just a small play/pause button,
                    // leaving the text the width of the screen.
                    compact = isReader && appState.readerFullscreen,
                    modifier = if (barHidden) Modifier.navigationBarsPadding() else Modifier,
                )
                if (!isReader && reciting == null) {
                    ResumePill(onOpen = openSection)
                }
            }
        }
    }
    }
}

/** A link in from the widget or a reminder notification. */
sealed interface DeepLink {
    data object Resume : DeepLink
    data object TipJar : DeepLink
    data class OpenVerse(val sectionId: String, val stanzaKey: String?) : DeepLink
}
