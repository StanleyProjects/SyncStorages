package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStoragesTest {
    @Test
    fun addTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.storage<String>(1)
        val p111 = testSuite.add(1, value = "v1")
        val s12 = testSuite.storage<Int>(1)
        val p121 = testSuite.add(1, value = 421)
        val s21 = testSuite.storage<String>(2)
        val p211 = testSuite.add(2, value = "v2")
        val s22 = testSuite.storage<Int>(2)
        val p221 = testSuite.add(2, value = 422)
        //
        assertEquals(expected = p111, actual = s11.payloads.single())
        assertEquals(
            expected = Payload(
                id = UUID(0, 0),
                created = 0.milliseconds,
                updated = 0.milliseconds,
                value = "v1",
            ),
            actual = p111,
        )
        //
        assertEquals(expected = p121, actual = s12.payloads.single())
        assertEquals(
            expected = Payload(
                id = UUID(0, 1),
                created = 1.milliseconds,
                updated = 1.milliseconds,
                value = 421,
            ),
            actual = p121,
        )
        //
        assertEquals(expected = p211, actual = s21.payloads.single())
        assertEquals(
            expected = Payload(
                id = UUID(0, 2),
                created = 2.milliseconds,
                updated = 2.milliseconds,
                value = "v2",
            ),
            actual = p211,
        )
        //
        assertEquals(expected = p221, actual = s22.payloads.single())
        assertEquals(
            expected = Payload(
                id = UUID(0, 3),
                created = 3.milliseconds,
                updated = 3.milliseconds,
                value = 422,
            ),
            actual = p221,
        )
    }

    @Test
    fun getSyncStatesTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.storage<String>(1)
        val p111 = testSuite.add(1, value = "v1")
        val s12 = testSuite.storage<Int>(1)
        val p121 = testSuite.add(1, value = 421)
        val s21 = testSuite.storage<String>(2)
        val p211 = testSuite.add(2, value = "v2")
        val s22 = testSuite.storage<Int>(2)
        val p221 = testSuite.add(2, value = 422)
        //
        assertEquals(
            expected = mapOf(
                s11.id to mockSyncState(
                    valueStates = mapOf(
                        p111.id to mockValueState(
                            hash = testSuite.hashOf(value = "v1"),
                            updated = 0.milliseconds,
                        ),
                    ),
                ),
                s12.id to mockSyncState(
                    valueStates = mapOf(
                        p121.id to mockValueState(
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
                        p211.id to mockValueState(
                            hash = testSuite.hashOf(value = "v2"),
                            updated = 2.milliseconds,
                        ),
                    ),
                ),
                s22.id to mockSyncState(
                    valueStates = mapOf(
                        p221.id to mockValueState(
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
        val s11 = testSuite.storage<String>(1)
        val p111 = testSuite.add(1, value = "v1")
        val s12 = testSuite.storage<Int>(1)
        val p121 = testSuite.add(1, value = 421)
        val s21 = testSuite.storage<String>(2)
        val p211 = testSuite.add(2, value = "v2")
        val s22 = testSuite.storage<Int>(2)
        val p221 = testSuite.add(2, value = 422)
        //
        assertEquals(
            expected = mapOf(
                s11.id to mockMergeState(
                    picks = setOf(p111.id),
                    gives = listOf(Transformers.Strings.map(payload = p211)),
                ),
                s12.id to mockMergeState(
                    picks = setOf(p121.id),
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
                    picks = setOf(p211.id),
                    gives = listOf(Transformers.Strings.map(payload = p111)),
                ),
                s22.id to mockMergeState(
                    picks = setOf(p221.id),
                    gives = listOf(Transformers.Ints.map(payload = p121)),
                ),
            ),
            actual = testSuite.s1.getMergeStates(syncStates = testSuite.s2.getSyncStates()),
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
    }
}
