package uk.tsundokus.features.orders.domain.series

import uk.tsundokus.features.orders.domain.models.Order
import kotlin.test.Test
import kotlin.test.assertEquals

private fun order(
    title: String,
    volume: String = "",
    isbn: String = "",
) = Order(id = "1", title = title, volume = volume, isbn = isbn)

class VolumeNumberTest {
    @Test
    fun `the volume field wins in any format`() {
        assertEquals(TitledVolume("One Piece", 12), VolumeNumber.parse(order("One Piece", volume = "Vol. 12")))
        assertEquals(TitledVolume("One Piece", 12), VolumeNumber.parse(order("One Piece", volume = "12")))
        assertEquals(TitledVolume("進撃の巨人", 3), VolumeNumber.parse(order("進撃の巨人", volume = "第3巻")))
    }

    @Test
    fun `a number at the end of the title is read when the field has none`() {
        assertEquals(TitledVolume("ONE PIECE", 107), VolumeNumber.parse(order("ONE PIECE 107")))
        assertEquals(TitledVolume("Naruto", 12), VolumeNumber.parse(order("Naruto, Vol. 12")))
        assertEquals(TitledVolume("進撃の巨人", 1), VolumeNumber.parse(order("進撃の巨人（1）")))
    }

    @Test
    fun `the same number in field and title is taken off the title`() {
        assertEquals(TitledVolume("One Piece", 107), VolumeNumber.parse(order("One Piece 107", volume = "107")))
    }

    @Test
    fun `a different number in the field leaves the title alone`() {
        assertEquals(TitledVolume("Blade 2", 5), VolumeNumber.parse(order("Blade 2", volume = "5")))
    }

    @Test
    fun `a bare number in a Western book's title is part of the title`() {
        val fahrenheit = order("Fahrenheit 451", isbn = "9781451673319")

        assertEquals(TitledVolume("Fahrenheit 451", null), VolumeNumber.parse(fahrenheit))
    }

    @Test
    fun `a marked number counts whatever the ISBN`() {
        assertEquals(
            TitledVolume("Berserk", 41),
            VolumeNumber.parse(order("Berserk Vol. 41", isbn = "9781506717869")),
        )
    }

    @Test
    fun `a title without a number has no volume`() {
        assertEquals(TitledVolume("Look Back", null), VolumeNumber.parse(order("Look Back")))
    }
}
