package uk.tsundokus.features.orders.presentation.addeditorder

import kotlinx.coroutines.CompletableDeferred
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.book.BookInfo
import uk.tsundokus.features.orders.domain.book.BookRepository

/** Answers from [books]; with [gate] set, every lookup waits for it, to test what happens meanwhile. */
internal class FakeBookRepository(
    private val books: Map<String, BookInfo> = emptyMap(),
    private val failing: Boolean = false,
    var gate: CompletableDeferred<Unit>? = null,
) : BookRepository {
    val lookedUp = mutableListOf<String>()

    override suspend fun lookup(isbn: String): Result<BookInfo?, DataError.Remote> {
        lookedUp += isbn
        gate?.await()
        return if (failing) Result.Failure(DataError.Remote.NO_INTERNET) else Result.Success(books[isbn])
    }
}
