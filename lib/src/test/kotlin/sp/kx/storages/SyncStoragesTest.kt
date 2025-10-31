package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.HexFormat
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStoragesTest {
    @Test
    fun addTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.require<String>(1)
        val p111 = testSuite.put(1, value = "v1")
        val s12 = testSuite.require<Int>(1)
        val p121 = testSuite.put(1, value = 421)
        val s21 = testSuite.require<String>(2)
        val p211 = testSuite.put(2, value = "v2")
        val s22 = testSuite.require<Int>(2)
        val p221 = testSuite.put(2, value = 422)
        //
        assertEquals(expected = p111, actual = s11.payloads.single())
        assertEquals(
            expected = Payload(
                value = "v1",
                valueInfo = ValueInfo(
                    id = UUID(0, 2),
                    created = 0.milliseconds,
                ),
                valueState = ValueState(
                    updated = 0.milliseconds,
                    hash = testSuite.hashOf(value = "v1"),
                ),
            ),
            actual = s11.payloads.single(),
        )
        //
        assertEquals(expected = p121, actual = s12.payloads.single())
        assertEquals(
            expected = Payload(
                value = 421,
                valueInfo = ValueInfo(
                    id = UUID(0, 3),
                    created = 1.milliseconds,
                ),
                valueState = ValueState(
                    updated = 1.milliseconds,
                    hash = testSuite.hashOf(value = 421),
                ),
            ),
            actual = s12.payloads.single(),
        )
        //
        assertEquals(expected = p211, actual = s21.payloads.single())
        assertEquals(
            expected = Payload(
                value = "v2",
                valueInfo = ValueInfo(
                    id = UUID(0, 4),
                    created = 2.milliseconds,
                ),
                valueState = ValueState(
                    updated = 2.milliseconds,
                    hash = testSuite.hashOf(value = "v2"),
                ),
            ),
            actual = s21.payloads.single(),
        )
        //
        assertEquals(expected = p221, actual = s22.payloads.single())
        assertEquals(
            expected = Payload(
                value = 422,
                valueInfo = ValueInfo(
                    id = UUID(0, 5),
                    created = 3.milliseconds,
                ),
                valueState = ValueState(
                    updated = 3.milliseconds,
                    hash = testSuite.hashOf(value = 422),
                ),
            ),
            actual = s22.payloads.single(),
        )
    }

    @Test
    fun getSyncStatesTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.require<String>(1)
        val p111 = testSuite.put(1, value = "v1")
        val s12 = testSuite.require<Int>(1)
        val p121 = testSuite.put(1, value = 421)
        val s21 = testSuite.require<String>(2)
        val p211 = testSuite.put(2, value = "v2")
        val s22 = testSuite.require<Int>(2)
        val p221 = testSuite.put(2, value = 422)
        //
        assertEquals(
            expected = mapOf(
                s11.id to mockSyncState(
                    valueStates = mapOf(
                        p111.valueInfo.id to mockValueState(
                            hash = testSuite.hashOf(value = "v1"),
                            updated = 0.milliseconds,
                        ),
                    ),
                ),
                s12.id to mockSyncState(
                    valueStates = mapOf(
                        p121.valueInfo.id to mockValueState(
                            hash = testSuite.hashOf(value = 421),
                            updated = 1.milliseconds,
                        ),
                    ),
                ),
            ),
            actual = testSuite.s1.getSyncStates(),
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
        //
        assertEquals(
            expected = mapOf(
                s21.id to mockSyncState(
                    valueStates = mapOf(
                        p211.valueInfo.id to mockValueState(
                            hash = testSuite.hashOf(value = "v2"),
                            updated = 2.milliseconds,
                        ),
                    ),
                ),
                s22.id to mockSyncState(
                    valueStates = mapOf(
                        p221.valueInfo.id to mockValueState(
                            hash = testSuite.hashOf(value = 422),
                            updated = 3.milliseconds,
                        ),
                    ),
                ),
            ),
            actual = testSuite.s2.getSyncStates(),
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
    }

    @Test
    fun getMergeStatesTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.require<String>(1)
        val p111 = testSuite.put(1, value = "v1")
        val s12 = testSuite.require<Int>(1)
        val p121 = testSuite.put(1, value = 421)
        val s21 = testSuite.require<String>(2)
        val p211 = testSuite.put(2, value = "v2")
        val s22 = testSuite.require<Int>(2)
        val p221 = testSuite.put(2, value = 422)
        //
        assertEquals(
            expected = mapOf(
                s11.id to mockMergeState(
                    picks = setOf(p111.valueInfo.id),
                    gives = listOf(Transformers.Strings.map(payload = p211)),
                ),
                s12.id to mockMergeState(
                    picks = setOf(p121.valueInfo.id),
                    gives = listOf(Transformers.Ints.map(payload = p221)),
                ),
            ),
            actual = testSuite.s2.getMergeStates(syncStates = testSuite.s1.getSyncStates()),
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
        //
        assertEquals(
            expected = mapOf(
                s21.id to mockMergeState(
                    picks = setOf(p211.valueInfo.id),
                    gives = listOf(Transformers.Strings.map(payload = p111)),
                ),
                s22.id to mockMergeState(
                    picks = setOf(p221.valueInfo.id),
                    gives = listOf(Transformers.Ints.map(payload = p121)),
                ),
            ),
            actual = testSuite.s2.getMergeStates(syncStates = testSuite.s1.getSyncStates()),
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
    }
}
