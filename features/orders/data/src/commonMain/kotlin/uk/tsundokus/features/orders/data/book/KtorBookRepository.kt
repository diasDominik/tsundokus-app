package uk.tsundokus.features.orders.data.book

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.networking.get
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.book.BookInfo
import uk.tsundokus.features.orders.domain.book.BookRepository

/**
 * Asks the server, which keeps one cache of the book databases for everyone. Answers — "unknown"
 * included — are also kept here for the session, so opening the same book again, or retyping an ISBN,
 * costs no request.
 */
@Single(binds = [BookRepository::class])
class KtorBookRepository(
    private val httpClient: HttpClient,
) : BookRepository {
    private val mutex = Mutex()

    // Insertion-ordered; the oldest entry goes first once the cap is reached.
    private val answers = LinkedHashMap<String, BookInfo?>()

    override suspend fun lookup(isbn: String): Result<BookInfo?, DataError.Remote> {
        mutex.withLock { if (isbn in answers) return Result.Success(answers[isbn]) }
        val result =
            httpClient.get<BookDto>(route = "/api/books/$isbn") {
                // The form is waiting on this; the 20 s default suits a sync, not someone typing.
                timeout { requestTimeoutMillis = LOOKUP_TIMEOUT_MILLIS }
            }
        val answer: Result<BookInfo?, DataError.Remote> =
            when (result) {
                is Result.Success -> {
                    Result.Success(result.data.toDomain())
                }

                is Result.Failure -> {
                    if (result.error == DataError.Remote.NOT_FOUND) Result.Success(null) else result
                }
            }
        // Failures are not kept: the next attempt might reach the server.
        if (answer is Result.Success) remember(isbn, answer.data)
        return answer
    }

    private suspend fun remember(
        isbn: String,
        info: BookInfo?,
    ) = mutex.withLock {
        answers[isbn] = info
        if (answers.size > MAX_ANSWERS) answers.remove(answers.keys.first())
    }

    private companion object {
        const val LOOKUP_TIMEOUT_MILLIS = 4_000L
        const val MAX_ANSWERS = 100
    }
}

private fun BookDto.toDomain(): BookInfo =
    BookInfo(
        isbn = isbn,
        title = title,
        author = author,
        publisher = publisher,
        volume = volume,
        releaseDate = releaseDate,
        hasCover = hasCover,
    )
