package uk.tsundokus.features.orders.presentation.widgets

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The iOS widget decodes this JSON in Swift (iosApp/TsundokuWidgets/WidgetSnapshot.swift). If this
 * test has to change, the Swift reader has to change with it.
 */
class WidgetSnapshotContractTest {
    @Test
    fun `the snapshot JSON keeps the shape the iOS widget reads`() {
        val snapshot =
            WidgetSnapshot(
                signedIn = true,
                arrivals = listOf(WidgetArrival("a1", "Frieren", "13", "2026-10-03", "SHIPPED")),
                unread = 12,
                reading = 2,
                oldestUnreadSince = "2026-09-03",
            )

        assertEquals(
            """{"signedIn":true,"arrivals":[{"id":"a1","title":"Frieren","volume":"13","date":"2026-10-03",""" +
                """"status":"SHIPPED"}],"unread":12,"reading":2,"oldestUnreadSince":"2026-09-03"}""",
            snapshot.encode(),
        )
    }

    @Test
    fun `the signed out snapshot writes every field too`() {
        assertEquals(
            """{"signedIn":false,"arrivals":[],"unread":0,"reading":0,"oldestUnreadSince":null}""",
            WidgetSnapshot.SignedOut.encode(),
        )
    }
}
