package uk.tsundokus.composeapp
import uk.tsundokus.core.designsystem.icon.TsundokuIcons

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import uk.tsundokus.composeapp.deeplink.DeepLinkHandler
import uk.tsundokus.composeapp.deeplink.buildDeepLinkMatchers
import uk.tsundokus.composeapp.deeplink.matchOrNull
import uk.tsundokus.composeapp.di.TsundokuKoinApp
import uk.tsundokus.composeapp.navigation.PlatformBackHandler
import uk.tsundokus.composeapp.navigation.rememberScreenTopBarNavEntryDecorator
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.presentation.navigation.FabAction
import uk.tsundokus.core.presentation.navigation.LocalTopBarActionsController
import uk.tsundokus.core.presentation.navigation.LoggedIn
import uk.tsundokus.core.presentation.navigation.ScreenWithFab
import uk.tsundokus.core.presentation.navigation.TopBarActionsController
import uk.tsundokus.core.presentation.navigation.TopLevelTab
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.authentication.presentation.navigation.SignIn
import uk.tsundokus.features.authentication.presentation.navigation.authGraph
import uk.tsundokus.features.authentication.presentation.navigation.authSerializersModule
import uk.tsundokus.features.orders.data.sync.OrderRealtimeSync
import uk.tsundokus.features.orders.presentation.navigation.AddOrder
import uk.tsundokus.features.orders.presentation.navigation.EditOrder
import uk.tsundokus.features.orders.presentation.navigation.OrderDetail
import uk.tsundokus.features.orders.presentation.navigation.Orders
import uk.tsundokus.features.orders.presentation.navigation.ReadingList
import uk.tsundokus.features.orders.presentation.navigation.ReportDelay
import uk.tsundokus.features.orders.presentation.navigation.ordersGraph
import uk.tsundokus.features.orders.presentation.navigation.ordersSerializersModule
import uk.tsundokus.features.settings.presentation.navigation.Settings
import uk.tsundokus.features.settings.presentation.navigation.settingsGraph
import uk.tsundokus.features.settings.presentation.navigation.settingsSerializersModule
import kotlinx.serialization.modules.plus
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel
import org.koin.plugin.module.dsl.koinConfiguration

private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = authSerializersModule + ordersSerializersModule + settingsSerializersModule
}

// Official Navigation3 deep-link matchers (DeepLinkRequest / UriDeepLinkMatcher) for the
// email-verification and password-reset links.
private val deepLinkMatchers = buildDeepLinkMatchers(devBaseUrl = BuildKonfig.BASE_URL_HTTP)

private const val NAV_TRANSITION_DURATION_MS = 300
private const val PREDICTIVE_POP_EXIT_TARGET_SCALE = 0.92f

// Fade both directions for ordinary navigate/pop.
private val navTransition: ContentTransform
    get() =
        ContentTransform(
            fadeIn(tween(NAV_TRANSITION_DURATION_MS)),
            fadeOut(tween(NAV_TRANSITION_DURATION_MS)),
        )

// All sub-animations share one duration: the predictive gesture scrubs the whole ContentTransform
// through a SeekableTransitionState, and mismatched durations hitch on settle.
private val predictivePopTransition: ContentTransform
    get() =
        ContentTransform(
            targetContentEnter = fadeIn(tween(NAV_TRANSITION_DURATION_MS)),
            initialContentExit =
                scaleOut(
                    targetScale = PREDICTIVE_POP_EXIT_TARGET_SCALE,
                    animationSpec = tween(NAV_TRANSITION_DURATION_MS),
                ) + fadeOut(tween(NAV_TRANSITION_DURATION_MS)),
        )

@Composable
private fun rememberEntryDecorators(backStack: NavBackStack<NavKey>): List<NavEntryDecorator<NavKey>> =
    listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
        // Last = innermost: the top bar composes inside the saveable-state/ViewModel scopes.
        rememberScreenTopBarNavEntryDecorator(backStack),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration<TsundokuKoinApp>(),
    ) {
        val mainViewModel = koinViewModel<MainViewModel>()

        // Hold the realtime order socket for the whole app lifetime; it connects while signed in and
        // reconnects on drops. Idempotent, so re-running on recomposition is a no-op.
        val koin = getKoin()
        LaunchedEffect(Unit) { koin.get<OrderRealtimeSync>().start() }

        val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
        val darkTheme =
            when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
        TsundokuTheme(darkTheme = darkTheme) {
            val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()

            // Wait for the persisted session to resolve before choosing a start screen,
            // otherwise a logged-in user briefly lands on (and is stuck at) sign-in.
            if (sessionState == SessionState.Loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@TsundokuTheme
            }

            val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
            val accountName by mainViewModel.accountName.collectAsStateWithLifecycle()
            val accountEmail by mainViewModel.accountEmail.collectAsStateWithLifecycle()

            val startDestination: NavKey = if (sessionState == SessionState.Authenticated) Orders else SignIn
            val backStack = rememberNavBackStack(configuration = savedStateConfiguration, startDestination)
            val currentKey = backStack.lastOrNull()

            // Bridge the browser Back button to the Nav3 back stack (web only; no-op elsewhere).
            // Enabled only when there is something to pop — at the root, Back keeps the user in the
            // app instead of unloading it.
            PlatformBackHandler(
                enabled = backStack.size > 1,
                onBack = { backStack.removeLastOrNull() },
            )

            // Drop to the sign-in screen when the session is invalidated (logout / refresh failure).
            ObserveAsEvents(mainViewModel.isLoggedIn) { loggedIn ->
                if (!loggedIn && currentKey is LoggedIn) {
                    backStack.clear()
                    backStack.add(SignIn)
                }
            }

            // Resolve incoming deep links (email verification / password reset) to a nav key and show it.
            // Both targets are auth-graph screens, so rebuild a homogeneous auth stack ([SignIn, key]) —
            // this keeps the auth NavDisplay free of logged-in keys it can't render, and works whether
            // or not a session exists (verification can happen either way).
            DisposableEffect(Unit) {
                DeepLinkHandler.listener = listener@{ uri ->
                    val navKey = deepLinkMatchers.matchOrNull(uri) ?: return@listener
                    backStack.clear()
                    backStack.add(SignIn)
                    backStack.add(navKey)
                }
                onDispose {
                    DeepLinkHandler.listener = null
                }
            }

            val topLevelTabs = remember { listOf(Orders, ReadingList, Settings) }
            val snackbarHostState = remember { SnackbarHostState() }

            if (currentKey is LoggedIn) {
                val topBarActions = remember { TopBarActionsController() }
                // Hide the navigation bar while typing: the keyboard already owns the bottom of the
                // screen, and stacking the bar on top of it would eat another ~88dp from the screen
                // the user is filling in.
                val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                val navigationSuiteType =
                    if (isKeyboardOpen) {
                        NavigationSuiteType.None
                    } else {
                        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
                    }
                NavigationSuiteScaffold(
                    // Lifts the whole shell above the keyboard. Applied here, at the node flush with
                    // the window, so the padding equals the keyboard height exactly.
                    modifier = Modifier.imePadding(),
                    navigationSuiteType = navigationSuiteType,
                    navigationItems = {
                        // The tab the stack is currently under, not just the top key: a detail entry
                        // stacked on a tab must keep that tab lit.
                        val activeTab = backStack.lastOrNull { it is TopLevelTab }
                        topLevelTabs.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationSuiteItem(
                                selected = selected,
                                onClick = {
                                    // Drop the whole current tab section, not just the tab key —
                                    // otherwise its detail entries outlive it and resurface
                                    // full-screen when backing out of the new tab.
                                    val tabIndex = backStack.indexOfLast { it is TopLevelTab }
                                    if (tabIndex >= 0) {
                                        while (backStack.size > tabIndex) backStack.removeLastOrNull()
                                    }
                                    backStack.add(tab)
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                                        contentDescription = tab.label.asString(),
                                    )
                                },
                                label = { Text(tab.label.asString()) },
                            )
                        }
                    },
                    primaryActionContent = {
                        val fabScreen = currentKey as? ScreenWithFab
                        if (fabScreen != null) {
                            FloatingActionButton(
                                modifier = Modifier.padding(start = 16.dp),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                onClick = {
                                    when (fabScreen.fabAction) {
                                        FabAction.AddOrder -> backStack.add(AddOrder)
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = TsundokuIcons.Add,
                                    contentDescription = "New order",
                                )
                            }
                        } else if (navigationSuiteType != NavigationSuiteType.NavigationBar) {
                            // Reserve the FAB's footprint so the rail/drawer items don't jump
                            // vertically when the FAB is shown/hidden across tabs.
                            Spacer(modifier = Modifier.padding(start = 16.dp).size(56.dp))
                        }
                    },
                ) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { padding ->
                        CompositionLocalProvider(LocalTopBarActionsController provides topBarActions) {
                            NavDisplay(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                backStack = backStack,
                                onBack = { backStack.removeLastOrNull() },
                                entryDecorators = rememberEntryDecorators(backStack),
                                transitionSpec = { navTransition },
                                popTransitionSpec = { navTransition },
                                predictivePopTransitionSpec = { _ -> predictivePopTransition },
                                entryProvider = entryProvider {
                                    ordersGraph(
                                        backStack = backStack,
                                        onOpenOrder = { backStack.add(OrderDetail(it)) },
                                        onAddOrder = { backStack.add(AddOrder) },
                                        onEditOrder = { backStack.add(EditOrder(it)) },
                                        onReportDelay = { backStack.add(ReportDelay(it)) },
                                        onBack = { backStack.removeLastOrNull() },
                                        snackbarHostState = snackbarHostState,
                                    )
                                    settingsGraph(
                                        backStack = backStack,
                                        onSignedOut = {
                                            mainViewModel.onDeliberateSignOut()
                                            backStack.clear()
                                            backStack.add(SignIn)
                                        },
                                        onBack = { backStack.removeLastOrNull() },
                                        snackbarHostState = snackbarHostState,
                                        accountName = accountName,
                                        accountEmail = accountEmail,
                                    )
                                },
                            )
                        }
                    }
                }
            } else {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (currentKey != SignIn) {
                            TopAppBar(
                                title = { },
                                navigationIcon = {
                                    IconButton(onClick = { backStack.removeLastOrNull() }) {
                                        Icon(
                                            imageVector = TsundokuIcons.ArrowBack,
                                            contentDescription = "Back",
                                        )
                                    }
                                },
                            )
                        }
                    },
                ) { padding ->
                    NavDisplay(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        backStack = backStack,
                        entryDecorators = rememberEntryDecorators(backStack),
                        transitionSpec = { navTransition },
                        popTransitionSpec = { navTransition },
                        predictivePopTransitionSpec = { _ -> predictivePopTransition },
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            authGraph(
                                backStack = backStack,
                                onAuthSuccess = {
                                    mainViewModel.reconcileAfterLogin()
                                    backStack.clear()
                                    backStack.add(Orders)
                                },
                                snackbarHostState = snackbarHostState,
                            )
                        },
                    )
                }
            }
        }
    }
}
