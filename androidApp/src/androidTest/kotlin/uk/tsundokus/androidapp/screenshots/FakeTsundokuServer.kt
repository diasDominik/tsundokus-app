package uk.tsundokus.androidapp.screenshots

import android.util.Log
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.rules.ExternalResource

/**
 * Stands in for the Tsundoku server while the store screenshots are taken. It listens on the
 * device's localhost:8080, the server a debug build talks to (BASE_URL_HTTP), and answers just
 * the calls the app makes on the way to the screenshotted screens.
 */
internal class FakeTsundokuServer(
    private val library: DemoLibrary = DemoLibrary(),
) : ExternalResource() {
    private val server = MockWebServer()

    override fun before() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = respond(request)
            }
        server.start(PORT)
    }

    override fun after() {
        server.close()
    }

    private fun respond(request: RecordedRequest): MockResponse {
        val path = request.url.encodedPath
        return when {
            path == "/api/auth/login" || path == "/api/auth/refresh" -> {
                json(authInfo())
            }

            path == "/api/auth/me" -> {
                json(user())
            }

            path == "/api/settings" -> {
                json(settings())
            }

            path == "/api/currencies" -> {
                json(currencies())
            }

            path == "/api/sync/orders" -> {
                json(library.syncResponse())
            }

            path == "/api/auth/passkeys" -> {
                json(JSONArray())
            }

            path.startsWith("/api/books/") && path.endsWith("/cover.jpg") -> {
                cover(path)
            }

            else -> {
                // Realtime sync (/ws/orders) among others: the app copes without it.
                Log.i(TAG, "No fake response for ${request.method} $path")
                MockResponse.Builder().code(404).build()
            }
        }
    }

    private fun cover(path: String): MockResponse {
        val isbn = path.removePrefix("/api/books/").removeSuffix("/cover.jpg")
        val order = library.order(isbn) ?: return MockResponse.Builder().code(404).build()
        return MockResponse
            .Builder()
            .code(200)
            .setHeader("Content-Type", "image/jpeg")
            .body(Buffer().write(DemoCovers.jpeg(order)))
            .build()
    }

    private fun json(body: Any): MockResponse =
        MockResponse
            .Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body(body.toString())
            .build()

    private fun user(): JSONObject =
        JSONObject()
            .put("id", "demo-user")
            .put("email", DEMO_EMAIL)
            .put("name", "Mika")
            .put("hasVerifiedEmail", true)

    private fun authInfo(): JSONObject =
        JSONObject()
            .put("accessToken", "demo-access-token")
            .put("refreshToken", "demo-refresh-token")
            .put("user", user())

    private fun settings(): JSONObject =
        JSONObject()
            .put("theme", "SYSTEM")
            .put("currency", "EUR")
            .put("currencyChosen", true)

    private fun currencies(): JSONArray =
        JSONArray()
            .put(currency("EUR", "€", "Euro", symbolAfterAmount = true))
            .put(currency("USD", "$", "US Dollar", symbolAfterAmount = false))
            .put(currency("GBP", "£", "British Pound", symbolAfterAmount = false))

    private fun currency(
        code: String,
        symbol: String,
        name: String,
        symbolAfterAmount: Boolean,
    ): JSONObject =
        JSONObject()
            .put("code", code)
            .put("symbol", symbol)
            .put("decimals", 2)
            .put("name", name)
            .put("symbolAfterAmount", symbolAfterAmount)

    companion object {
        const val PORT = 8080
        const val DEMO_EMAIL = "mika@example.com"
        const val DEMO_PASSWORD = "demo-pass-1"
        private const val TAG = "FakeTsundokuServer"
    }
}
