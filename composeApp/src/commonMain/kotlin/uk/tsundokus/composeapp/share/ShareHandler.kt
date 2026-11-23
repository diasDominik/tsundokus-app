package uk.tsundokus.composeapp.share

/**
 * Platform bridge for what another app shares into Tsundoku (Android's share sheet): a screenshot or a
 * piece of text, which starts a new order with the ISBN read out of it. Like the deep-link bridge, a
 * share that arrives before the app's UI listens is kept and delivered once it does.
 */
object ShareHandler {
    sealed interface Shared {
        data class Text(val text: String) : Shared

        /** A content URI the sharing app granted read access to. */
        data class Image(val uri: String) : Shared
    }

    private var cached: Shared? = null

    var listener: ((Shared) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                cached?.let(value)
                cached = null
            }
        }

    fun onShared(shared: Shared) {
        val current = listener
        if (current == null) cached = shared else current(shared)
    }
}
