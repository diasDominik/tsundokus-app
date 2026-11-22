package uk.tsundokus.androidapp.screenshots

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import uk.tsundokus.androidapp.MainActivity

/**
 * The Play Store screenshots. fastlane runs this once per store language (`fastlane screenshots`),
 * passing the language as the `testLocale` argument; screengrab saves each capture under it.
 *
 * The app talks to [FakeTsundokuServer] and shows [DemoLibrary], so it needs a debug build that
 * points at http://localhost:8080 and nothing else running on that port.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class StoreScreenshots {
    private val library = DemoLibrary()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    // The server and the language have to be in place before the activity starts.
    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(FakeTsundokuServer(library))
            .around(AppLocaleRule())
            .around(composeRule)

    @Test
    fun storeScreenshots() {
        signInIfNeeded()
        val featured = library.featured
        composeRule.waitUntilAtLeastOneExists(hasText(featured.series.title), TIMEOUT)
        // Orders arriving from the sync can push the first rows out of view: start at the top.
        val orderList =
            composeRule.onNode(hasScrollToIndexAction() and hasAnyDescendant(hasText(featured.series.title)))
        orderList.performScrollToIndex(0)
        capture("1_orders")

        orderList.performScrollToNode(hasText(featured.series.title))
        composeRule.onAllNodes(hasText(featured.series.title)).onFirst().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("${featured.series.title} ${featured.volume}"), TIMEOUT)
        capture("2_order_detail")
        Espresso.pressBack()

        openTab("SeriesList")
        capture("3_series")
        openTab("ReadingList")
        capture("4_reading")
        openTab("Stats")
        capture("5_stats")
    }

    private fun signInIfNeeded() {
        composeRule.waitUntil(TIMEOUT) { exists(hasTestTag("login:email")) || exists(hasTestTag("tab:Orders")) }
        if (!exists(hasTestTag("login:email"))) return

        textField("login:email").performTextInput(FakeTsundokuServer.DEMO_EMAIL)
        textField("login:password").performTextInput(FakeTsundokuServer.DEMO_PASSWORD)
        // The keyboard can cover the button, so invoke its click action rather than tapping it.
        Espresso.closeSoftKeyboard()
        composeRule.onNode(hasTestTag("login:submit")).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntilAtLeastOneExists(hasTestTag("tab:Orders"), TIMEOUT)
    }

    private fun textField(tag: String) =
        composeRule.onNode(hasSetTextAction() and (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag))))

    private fun openTab(name: String) {
        composeRule.onNode(hasTestTag("tab:$name")).performClick()
    }

    private fun exists(matcher: androidx.compose.ui.test.SemanticsMatcher): Boolean =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun capture(name: String) {
        composeRule.waitForIdle()
        // Covers load asynchronously after the screen settles.
        SystemClock.sleep(SETTLE_MILLIS)
        Screengrab.screenshot(name)
    }

    /** Switches the app to the language fastlane passes as `testLocale`, e.g. de-DE. */
    private class AppLocaleRule : ExternalResource() {
        override fun before() {
            val locale = InstrumentationRegistry.getArguments().getString("testLocale") ?: return
            check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "Store screenshots set the app language per app, which needs Android 13 or newer"
            }
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(locale)
        }
    }

    companion object {
        private const val TIMEOUT = 20_000L
        private const val SETTLE_MILLIS = 1_500L

        @JvmStatic
        @BeforeClass
        fun useUiAutomator() {
            // Captures the whole screen, status and navigation bar included, as a user sees it.
            Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        }
    }
}
