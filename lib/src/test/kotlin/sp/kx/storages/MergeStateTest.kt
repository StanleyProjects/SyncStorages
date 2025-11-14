package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class MergeStateTest {
    @Test
    fun toStringTest() {
        val payload = Payload(
            id = UUID(1, 0),
            created = 1.milliseconds,
            updated = 2.milliseconds,
            value = byteArrayOf(4, 3, 2, 1),
        )
        val issuer = MergeState(
            deleted = setOf(UUID(2, 0)),
            picks = setOf(UUID(3, 0)),
            gives = listOf(payload),
        )
        val expected = "MergeState(deleted: [00000000-0000-0002-0000-000000000000], picks: [00000000-0000-0003-0000-000000000000], gives: 1)"
        assertEquals(expected, issuer.toString())
    }

    @Test
    fun hashCodeTest() {
        val payload = Payload(
            id = UUID(1, 0),
            created = 1.milliseconds,
            updated = 2.milliseconds,
            value = byteArrayOf(4, 3, 2, 1),
        )
        val issuer = MergeState(
            deleted = setOf(UUID(2, 0)),
            picks = setOf(UUID(3, 0)),
            gives = listOf(payload),
        )
        val expected = 2048061532
        assertEquals(expected, issuer.hashCode())
    }
}
