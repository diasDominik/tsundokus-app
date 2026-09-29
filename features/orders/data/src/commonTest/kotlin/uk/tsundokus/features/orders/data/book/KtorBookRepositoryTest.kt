package uk.tsundokus.features.orders.data.book

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.book.BookInfo
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ISBN = "9784088820453"

class KtorBookRepositoryTest {
    private var requests = 0

    private fun repository(
        status: HttpStatusCode,
        body: String = "",
    ): KtorBookRepository {
        val engine =
            MockEngine {
                requests++
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        val client =
            HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(HttpTimeout)
            }
        return KtorBookRepository(client)
    }

    @Test
    fun `a found book is returned and not asked for again`() =
        runTest {
            val sut =
                repository(
                    HttpStatusCode.OK,
                    """{"isbn": "$ISBN", "title": "姫様", "volume": "1", "hasCover": true}""",
                )

            val expected = Result.Success(BookInfo(isbn = ISBN, title = "姫様", volume = "1", hasCover = true))
            assertEquals(expected, sut.lookup(ISBN))
            assertEquals(expected, sut.lookup(ISBN))
            assertEquals(1, requests)
        }

    @Test
    fun `an unknown book is an empty answer and is not asked for again`() =
        runTest {
            val sut = repository(HttpStatusCode.NotFound)

            assertEquals(Result.Success(null), sut.lookup(ISBN))
            assertEquals(Result.Success(null), sut.lookup(ISBN))
            assertEquals(1, requests)
        }

    @Test
    fun `a failure is reported and the next lookup tries again`() =
        runTest {
            val sut = repository(HttpStatusCode.ServiceUnavailable)

            assertEquals(Result.Failure(DataError.Remote.SERVICE_UNAVAILABLE), sut.lookup(ISBN))
            sut.lookup(ISBN)
            assertEquals(2, requests)
        }
}
