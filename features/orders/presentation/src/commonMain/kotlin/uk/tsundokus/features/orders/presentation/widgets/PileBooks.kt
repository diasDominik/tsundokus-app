package uk.tsundokus.features.orders.presentation.widgets

import kotlinx.serialization.Serializable
import uk.tsundokus.features.orders.domain.dates.daysBetween

/** One unread volume in the pile widget's drawing. */
@Serializable
data class WidgetPileBook(
    val id: String,
    /** When it arrived (ISO `yyyy-MM-dd`); blank when that was never recorded. */
    val since: String,
    /** Being read: pulled half out of the pile, bookmark in, no dust. */
    val reading: Boolean = false,
)

/** How neglected a volume in the pile looks, from how long it has waited. */
enum class DustStage {
    /** Under two weeks: fresh. */
    CLEAN,

    /** Two weeks: a film of dust on top. */
    LIGHT,

    /** A month: thick dust, the colour going grey. */
    THICK,

    /** Two months: cobwebs between it and the next one. */
    COBWEB,

    /** Four months: bleached, sagging, and a spider has moved in. */
    SPIDER,
}

internal const val LIGHT_DUST_FROM_DAYS = 14
internal const val THICK_DUST_FROM_DAYS = 30
internal const val COBWEB_FROM_DAYS = 60
internal const val SPIDER_FROM_DAYS = 120

/**
 * How [this] volume looks on [today]. Worked out while drawing, not stored, so the dust keeps
 * settling while the app stays closed. One being read was just picked up: no dust. One with no
 * arrival date is left clean rather than guessed at.
 */
fun WidgetPileBook.dustOn(today: String): DustStage {
    if (reading) return DustStage.CLEAN
    val days = daysBetween(since, today) ?: return DustStage.CLEAN
    return when {
        days >= SPIDER_FROM_DAYS -> DustStage.SPIDER
        days >= COBWEB_FROM_DAYS -> DustStage.COBWEB
        days >= THICK_DUST_FROM_DAYS -> DustStage.THICK
        days >= LIGHT_DUST_FROM_DAYS -> DustStage.LIGHT
        else -> DustStage.CLEAN
    }
}

/**
 * What tells one volume from the next in the drawing, picked from its id so a book looks the same
 * every time the widget redraws. Fractions are of the pile's width or of a spine's thickness.
 */
data class PileBookLook(
    /** Index into the drawing's spine colours. */
    val colour: Int,
    /** How long the spine is, of the pile's width. */
    val length: Float,
    /** How far its middle sits from the pile's middle, of the pile's width; negative is left. */
    val shift: Float,
    /** Thicker or thinner than the average volume. */
    val thickness: Float,
    /** Lies the other way round: its page edges face out instead of its spine. */
    val pagesOut: Boolean,
    /** Seed for the dust grains, so they don't move between redraws. */
    val seed: Long,
)

fun WidgetPileBook.look(colours: Int): PileBookLook {
    val hash = fnv1a(id)

    fun part(
        shiftBits: Int,
        steps: Int,
    ): Int = ((hash ushr shiftBits) and 0xFFFF).toInt() % steps
    return PileBookLook(
        colour = part(0, colours),
        length = 0.72f + part(8, 25) / 100f,
        shift = (part(16, 17) - 8) / 100f,
        thickness = 0.8f + part(24, 41) / 100f,
        pagesOut = part(32, 5) == 0,
        seed = hash,
    )
}

/** FNV-1a, 64 bit: the same on every platform, unlike [String.hashCode] across Kotlin targets. */
private fun fnv1a(text: String): Long {
    var hash = -0x340d631b7bdddcdbL
    for (char in text) {
        hash = hash xor char.code.toLong()
        hash *= 0x100000001b3L
    }
    return hash
}
