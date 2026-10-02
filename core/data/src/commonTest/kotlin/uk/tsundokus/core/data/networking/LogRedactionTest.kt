package uk.tsundokus.core.data.networking

import kotlin.test.Test
import kotlin.test.assertEquals

class LogRedactionTest {
    @Test
    fun `the websocket token and api key are masked in a url`() {
        assertEquals(
            "REQUEST: wss://tsundokus.uk/ws/orders?token=***&apiKey=***",
            redactSecrets(
                "REQUEST: wss://tsundokus.uk/ws/orders?token=eyJhbGciOiJIUzI1NiJ9.e30.sig&apiKey=3925410be4",
            ),
        )
    }

    @Test
    fun `a masked query keeps the parameters around it`() {
        assertEquals(
            "FROM: https://x/verify?email=a%40b.c&token=***#done",
            redactSecrets("FROM: https://x/verify?email=a%40b.c&token=abc123#done"),
        )
    }

    @Test
    fun `passwords in a request body are masked`() {
        assertEquals(
            """{"email":"reader@example.com","password":"***"}""",
            redactSecrets("""{"email":"reader@example.com","password":"hunter2"}"""),
        )
        assertEquals(
            """{"currentPassword":"***","newPassword":"***"}""",
            redactSecrets("""{"currentPassword":"old","newPassword":"new"}"""),
        )
    }

    @Test
    fun `session tokens in a response body are masked`() {
        assertEquals(
            """{"accessToken":"***", "refreshToken":"***","userId":"u1"}""",
            redactSecrets("""{"accessToken": "a.b.c", "refreshToken" : "r-1","userId":"u1"}"""),
        )
    }

    @Test
    fun `an escaped quote does not end the masked value early`() {
        assertEquals(
            """{"password":"***","email":"a@b.c"}""",
            redactSecrets("""{"password":"say \"hi\"","email":"a@b.c"}"""),
        )
    }

    @Test
    fun `fields that only end in token are left alone`() {
        val line = """{"tokenType":"Bearer","pageToken":"p2"}"""

        assertEquals(line, redactSecrets(line))
    }

    @Test
    fun `lines without secrets are unchanged`() {
        val line = "RESPONSE: 200 OK METHOD: GET FROM: https://tsundokus.uk/api/orders?since=0"

        assertEquals(line, redactSecrets(line))
    }
}
