package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

internal class ValueStateTest {
    @Test
    fun toStringTest() {
        val issuer = ValueState(
            updated = 1.milliseconds,
            hash = byteArrayOf(4, 3, 2, 1),
        )
        val expected = "ValueState(updated: 1, hash: 4)"
        assertEquals(expected, issuer.toString())
    }

    @Test
    fun hashCodeTest() {
        val issuer = ValueState(
            updated = 1.milliseconds,
            hash = byteArrayOf(4, 3, 2, 1),
        )
        val expected = 63046592
        assertEquals(expected, issuer.hashCode())
    }

    @Test
    fun equalsTest() {
        assertTrue(
            ValueState(
                updated = 1.milliseconds,
                hash = byteArrayOf(4, 3, 2, 1),
            ) == ValueState(
                updated = 1.milliseconds,
                hash = byteArrayOf(4, 3, 2, 1),
            ),
        )
        assertTrue(
            ValueState(
                updated = 2.milliseconds,
                hash = byteArrayOf(4, 3, 2, 1),
            ) != ValueState(
                updated = 1.milliseconds,
                hash = byteArrayOf(4, 3, 2, 1),
            ),
        )
        assertTrue(
            ValueState(
                updated = 1.milliseconds,
                hash = byteArrayOf(4, 3, 2),
            ) != ValueState(
                updated = 1.milliseconds,
                hash = byteArrayOf(4, 3, 2, 1),
            ),
        )
        assertTrue(
            !ValueState(
                updated = 1.milliseconds,
                hash = byteArrayOf(4, 3, 2, 1),
            ).equals(Unit),
        )
    }
}
