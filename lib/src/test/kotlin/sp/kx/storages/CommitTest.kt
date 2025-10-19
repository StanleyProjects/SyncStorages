package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.hashes.Hashes

internal class CommitTest {
    @Test
    fun commitTest() {
        val transformer = StringTransformer
        val s1 = mockSyncStorage(
            transformer = transformer,
            hashes = Hashes.MD5,
        )
        val p11 = s1.add("v11")
        val p12 = s1.add("v12")
        val p13 = s1.add("v13")
        //
        val s2 = mockSyncStorage(
            transformer = transformer,
            hashes = Hashes.MD5,
        )
        val p21 = s2.add("v21")
        val p22 = s2.add("v22")
        val p23 = s2.add("v23")
        //
        val syncState = s1.getSyncState()
        assertTrue(syncState.deleted.isEmpty())
        syncState.valueStates.also { valueStates ->
            val expected = mapOf(
                p11.valueInfo.id to p11.valueState,
                p12.valueInfo.id to p12.valueState,
                p13.valueInfo.id to p13.valueState,
            )
            assertEquals(
                expected = expected,
                actual = valueStates,
                assert = { expected, actual ->
                    assertEquals(expected.updated, actual.updated)
                    assertTrue(expected.hash.contentEquals(actual.hash))
                },
            )
        }
        //
        val mergeState = s2.getMergeState(syncState = syncState)
        assertTrue(mergeState.deleted.isEmpty())
        assertEquals(
            expected = setOf(p11.valueInfo.id, p12.valueInfo.id, p13.valueInfo.id),
            actual = mergeState.downloaded,
        )
        mergeState.encoded.also { encoded ->
            val expected = listOf(p21, p22, p23)
            assertEquals(
                expected = expected,
                actual = encoded.map {
                    Payload(
                        value = transformer.decode(it.value),
                        valueInfo = it.valueInfo,
                        valueState = it.valueState,
                    )
                },
                comparator = Comparators.payloads,
                assert = { index, expected, actual ->
                    assertEquals(expected = expected, actual = actual, message = "index: $index")
                },
            )
        }
        //
        val commitState = s1.merge(mergeState = mergeState)
        assertTrue(commitState.deleted.isEmpty())
        commitState.encoded.also { encoded ->
            val expected = listOf(p11, p12, p13)
            assertEquals(
                expected = expected,
                actual = encoded.map {
                    Payload(
                        value = transformer.decode(it.value),
                        valueInfo = it.valueInfo,
                        valueState = it.valueState,
                    )
                },
                comparator = Comparators.payloads,
                assert = { index, expected, actual ->
                    assertEquals(expected = expected, actual = actual, message = "index: $index")
                },
            )
        }
        //
        assertTrue(s2.commit(commitState = commitState))
        s1.items.also { items ->
            val expected = listOf(p11, p12, p13, p21, p22, p23)
            assertEquals(
                expected = expected,
                actual = items,
                comparator = Comparators.payloads,
                assert = { index, expected, actual ->
                    assertEquals(expected = expected, actual = actual, message = "index: $index")
                },
            )
        }
        //
        s1.delete(p11.valueInfo.id)
        val vs12 = s1.set(p12.valueInfo.id, "v12_updated")
        checkNotNull(vs12)
        val p12u = Payload(
            value = "v12_updated",
            valueState = vs12,
            valueInfo = p12.valueInfo,
        )
        s2.delete(p21.valueInfo.id)
        val vs22 = s2.set(p22.valueInfo.id, "v22_updated")
        checkNotNull(vs22)
        val p22u = Payload(
            value = "v22_updated",
            valueState = vs22,
            valueInfo = p22.valueInfo,
        )
        //
        assertTrue(s2.commit(commitState = s1.merge(mergeState = s2.getMergeState(syncState = s1.getSyncState()))))
        s1.items.also { items ->
            val expected = listOf(p12u, p13, p22u, p23)
            assertEquals(
                expected = expected,
                actual = items,
                comparator = Comparators.payloads,
                assert = { index, expected, actual ->
                    assertEquals(expected = expected, actual = actual, message = "index: $index")
                },
            )
        }
    }
}
