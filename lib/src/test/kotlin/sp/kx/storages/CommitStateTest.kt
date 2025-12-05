package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class CommitStateTest {
    @Test
    fun toStringTest() {
        val payload = Payload(
            id = UUID(1, 0),
            created = 1.milliseconds,
            updated = 2.milliseconds,
            value = byteArrayOf(4, 3, 2, 1),
        )
        val issuer = CommitState(
            deleted = setOf(UUID(2, 0)),
            gives = listOf(payload),
            hash = byteArrayOf(4, 2),
        )
        val expected = "CommitState(deleted: [00000000-0000-0002-0000-000000000000], gives: 1, hash: 2)"
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
        val issuer = CommitState(
            deleted = setOf(UUID(2, 0)),
            gives = listOf(payload),
            hash = byteArrayOf(4, 2),
        )
        val expected = -936478655
        assertEquals(expected, issuer.hashCode())
    }

    @Test
    fun equalsTest() {
        val payload = Payload(
            id = UUID(1, 0),
            created = 1.milliseconds,
            updated = 2.milliseconds,
            value = byteArrayOf(4, 3, 2, 1),
        )
        assertTrue(
            CommitState(
                deleted = setOf(UUID(2, 0)),
                gives = listOf(payload),
                hash = byteArrayOf(4, 2),
            ) == CommitState(
                deleted = setOf(UUID(2, 0)),
                gives = listOf(payload),
                hash = byteArrayOf(4, 2),
            ),
        )
        assertTrue(
            CommitState(
                deleted = setOf(),
                gives = listOf(payload),
                hash = byteArrayOf(4, 2),
            ) != CommitState(
                deleted = setOf(UUID(2, 0)),
                gives = listOf(payload),
                hash = byteArrayOf(4, 2),
            ),
        )
        assertTrue(
            CommitState(
                deleted = setOf(UUID(2, 0)),
                gives = listOf(),
                hash = byteArrayOf(4, 2),
            ) != CommitState(
                deleted = setOf(UUID(2, 0)),
                gives = listOf(payload),
                hash = byteArrayOf(4, 2),
            ),
        )
        assertTrue(
            CommitState(
                deleted = setOf(UUID(2, 0)),
                gives = listOf(payload),
                hash = byteArrayOf(),
            ) != CommitState(
                deleted = setOf(UUID(2, 0)),
                gives = listOf(payload),
                hash = byteArrayOf(4, 2),
            ),
        )
        assertTrue(
            !CommitState(
                deleted = setOf(UUID(2, 0)),
                gives = listOf(payload),
                hash = byteArrayOf(4, 2),
            ).equals(Unit),
        )
    }
}
