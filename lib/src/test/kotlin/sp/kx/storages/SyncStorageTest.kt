package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.hashes.Hashes
import java.util.HexFormat

internal class SyncStorageTest {
    @Test
    fun syncStateTest() {
        val storage = mockSyncStorage(
            transformer = StringTransformer,
            hashes = Hashes.MD5,
        )
        val value = "foo bar baz"
        val payload = storage.add(value = value)
        assertEquals(value, payload.value)
        val expected = HexFormat.of().parseHex("ab07acbb1e496801937adfa772424bf7")
        assertTrue(expected.contentEquals(payload.valueState.hash))
        //
        val actual = storage[payload.valueInfo.id]
        checkNotNull(actual)
        assertEquals(payload.value, actual.value)
        assertEquals(payload.valueInfo, actual.valueInfo)
        assertEquals(payload.valueState.updated, actual.valueState.updated)
        assertTrue(payload.valueState.hash.contentEquals(actual.valueState.hash))
        //
        storage.syncState.also { syncState ->
            assertTrue(syncState.deleted.isEmpty())
            val (id, valueState) = syncState.valueStates.entries.single()
            assertEquals(payload.valueInfo.id, id)
            assertEquals(valueState.updated, payload.valueState.updated)
            assertTrue(valueState.hash.contentEquals(payload.valueState.hash))
        }
        //
        assertTrue(storage.delete(payload.valueInfo.id))
        assertNull(storage[payload.valueInfo.id])
        //
        storage.syncState.also { syncState ->
            assertEquals(payload.valueInfo.id, syncState.deleted.single())
            assertTrue(syncState.valueStates.isEmpty())
        }
    }
}
