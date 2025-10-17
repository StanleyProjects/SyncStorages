package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.hashes.Hashes
import java.util.HexFormat
import java.util.UUID

internal class SyncStorageTest {
    @Test
    fun getSyncStateTest() {
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
        storage.getSyncState().also { syncState ->
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
        storage.getSyncState().also { syncState ->
            assertEquals(payload.valueInfo.id, syncState.deleted.single())
            assertTrue(syncState.valueStates.isEmpty())
        }
    }

    @Test
    fun getMergeStateTest() {
        val s1 = mockSyncStorage(
            transformer = StringTransformer,
            hashes = Hashes.MD5,
        )
        val p11 = s1.add("v11")
        val p12 = s1.add("v12")
        s1.delete(p11.valueInfo.id)
        //
        val s2 = mockSyncStorage(
            transformer = StringTransformer,
            hashes = Hashes.MD5,
        )
        val p21 = s2.add("v21")
        val p22 = s2.add("v22")
        s2.delete(p21.valueInfo.id)
        //
        val syncState = s1.getSyncState()
        assertEquals(p11.valueInfo.id, syncState.deleted.single())
        val mergeState = s2.getMergeState(syncState = syncState)
        assertEquals(p21.valueInfo.id, mergeState.deleted.single())
        TODO("SyncStorageTest:getMergeStateTest")
    }
}
