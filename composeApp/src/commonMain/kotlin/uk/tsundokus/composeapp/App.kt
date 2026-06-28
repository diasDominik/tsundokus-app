package uk.tsundokus.composeapp
import uk.tsundokus.core.designsystem.icon.TsundokuIcons

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
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
import uk.tsundokus.composeapp.navigation.ScreenTopBar
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.presentation.navigation.FabAction
import uk.tsundokus.core.presentation.navigation.LocalTopBarActionsController
import uk.tsundokus.core.presentation.navigation.LoggedIn
import uk.tsundokus.core.presentation.navigation.ScreenWithFab
import uk.tsundokus.core.presentation.navigation.ScreenWithTopBar
import uk.tsundokus.core.presentation.navigation.TopBarActionsController
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.authentication.presentation.navigation.SignIn
import uk.tsundokus.features.authentication.presentation.navigation.authGraph
import uk.tsundokus.features.authentication.presentation.navigation.authSerializersModule
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.plugin.module.dsl.koinConfiguration

private val savedStateConfiguration = SavedStateConfiguration {
    serializersModule = authSerializersModule + ordersSerializersModule + settingsSerializersModule
}

// Official Navigation3 deep-link matchers (DeepLinkRequest / UriDeepLinkMatcher) for the
// email-verification and password-reset links.
private val deepLinkMatchers = buildDeepLinkMatchers(devBaseUrl = BuildKonfig.BASE_URL_HTTP)

@get:Composable
private val entryDecorators
    get() = listOf<NavEntryDecorator<NavKey>>(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration<TsundokuKoinApp>(),
    ) {
        val mainViewModel = koinViewModel<MainViewModel>()
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
                NavigationSuiteScaffold(
                    navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2()),
                    navigationItems = {
                        topLevelTabs.forEach { tab ->
                            val selected = currentKey == tab
                            NavigationSuiteItem(
                                selected = selected,
                                onClick = {
                                    backStack.removeAll { it is uk.tsundokus.core.presentation.navigation.TopLevelTab }
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
                        (currentKey as? ScreenWithFab)?.let { screen ->
                            FloatingActionButton(
                                modifier = Modifier.padding(start = 16.dp),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                onClick = {
                                    when (screen.fabAction) {
                                        FabAction.AddOrder -> backStack.add(AddOrder)
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = TsundokuIcons.Add,
                                    contentDescription = "New order",
                                )
                            }
                        }
                    },
                ) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            val override = topBarActions.overrideFor(currentKey)
                            if (override != null) {
                                ScreenTopBar(
                                    title = override.title,
                                    navigationAction = override.navigationAction,
                                    onNavigationClick = override.onNavigationClick,
                                    actions = override.actions,
                                )
                            } else {
                                (currentKey as? ScreenWithTopBar)?.let { config ->
                                    ScreenTopBar(
                                        title = config.topBarTitle,
                                        navigationAction = config.topBarAction,
                                        onNavigationClick = { backStack.removeLastOrNull() },
                                        actions = { topBarActions.actionsFor(currentKey)?.invoke() },
                                    )
                                }
                            }
                        },
                    ) { padding ->
                        CompositionLocalProvider(LocalTopBarActionsController provides topBarActions) {
                            NavDisplay(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                backStack = backStack,
                                onBack = { backStack.removeLastOrNull() },
                                entryDecorators = entryDecorators,
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
                        entryDecorators = entryDecorators,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            authGraph(
                                backStack = backStack,
                                onAuthSuccess = {
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
