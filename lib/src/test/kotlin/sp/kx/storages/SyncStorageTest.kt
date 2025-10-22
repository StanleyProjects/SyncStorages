package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.util.HexFormat

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
        assertEquals(expected = payload, actual = actual)
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
        val transformer: Transformer<String> = StringTransformer
        val hashes: Hashes = Hashes.MD5
        val times: Times = MockTimes()
        val ids: Ids = MockIds()
        //
        val s1 = mockSyncStorage(
            transformer = transformer,
            hashes = hashes,
            times = times,
            ids = ids,
        )
        val p11 = s1.add("v11")
        val p12 = s1.add("v12")
        s1.delete(p11.valueInfo.id)
        //
        val s2 = mockSyncStorage(
            transformer = transformer,
            hashes = hashes,
            times = times,
            ids = ids,
        )
        val p21 = s2.add("v21")
        val p22 = s2.add("v22")
        s2.delete(p21.valueInfo.id)
        //
        val syncState = s1.getSyncState()
        assertEquals(p11.valueInfo.id, syncState.deleted.single())
        assertEquals(p12.valueInfo.id, syncState.valueStates.entries.single().key)
        //
        val mergeState = s2.getMergeState(syncState = syncState)
        assertEquals(p21.valueInfo.id, mergeState.deleted.single())
        assertEquals(p12.valueInfo.id, mergeState.downloaded.single())
        assertEquals(p22.valueInfo.id, mergeState.encoded.single().valueInfo.id)
    }

    @Test
    fun mergeTest() {
        val transformer: Transformer<String> = StringTransformer
        val hashes: Hashes = Hashes.MD5
        val times: Times = MockTimes()
        val ids: Ids = MockIds()
        //
        val s1 = mockSyncStorage(
            transformer = transformer,
            hashes = hashes,
            times = times,
            ids = ids,
        )
        val p11 = s1.add("v11")
        val p12 = s1.add("v12")
        s1.delete(p11.valueInfo.id)
        //
        val s2 = mockSyncStorage(
            transformer = transformer,
            hashes = hashes,
            times = times,
            ids = ids,
        )
        val p21 = s2.add("v21")
        val p22 = s2.add("v22")
        s2.delete(p21.valueInfo.id)
        //
        val syncState = s1.getSyncState()
        val mergeState = s2.getMergeState(syncState = syncState)
        val commitState = s1.merge(mergeState = mergeState)
        assertEquals(p11.valueInfo.id, commitState.deleted.single())
        val payload = commitState.encoded.single()
        assertEquals(transformer.decode(payload.value), p12.value)
        assertEquals(payload.valueInfo, p12.valueInfo)
        assertEquals(payload.valueState.updated, p12.valueState.updated)
        assertTrue(payload.valueState.hash.contentEquals(p12.valueState.hash))
        //
        val payloads = s1.payloads
        assertEquals(2, payloads.size)
        val (p1, p2) = payloads
        assertEquals(expected = p12, actual = p1)
        assertEquals(expected = p22, actual = p2)
    }

    @Test
    fun commitTest() {
        val transformer: Transformer<String> = StringTransformer
        val hashes: Hashes = Hashes.MD5
        val times: Times = MockTimes()
        val ids: Ids = MockIds()
        //
        val s1 = mockSyncStorage(
            transformer = transformer,
            hashes = hashes,
            times = times,
            ids = ids,
        )
        val p11 = s1.add("v11")
        val p12 = s1.add("v12")
        s1.delete(p11.valueInfo.id)
        //
        val s2 = mockSyncStorage(
            transformer = transformer,
            hashes = hashes,
            times = times,
            ids = ids,
        )
        val p21 = s2.add("v21")
        val p22 = s2.add("v22")
        s2.delete(p21.valueInfo.id)
        //
        val syncState = s1.getSyncState()
        val mergeState = s2.getMergeState(syncState = syncState)
        val commitState = s1.merge(mergeState = mergeState)
        assertTrue(s2.commit(commitState = commitState))
        s1.payloads.also { payloads ->
            assertEquals(2, payloads.size)
            val (p1, p2) = payloads
            assertEquals(expected = p12, actual = p1)
            assertEquals(expected = p22, actual = p2)
        }
        s2.payloads.also { payloads ->
            assertEquals(2, payloads.size)
            val (p1, p2) = payloads
            assertEquals(expected = p12, actual = p1)
            assertEquals(expected = p22, actual = p2)
        }
    }
}
