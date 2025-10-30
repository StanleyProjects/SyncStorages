package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        val s11 = testSuite.s1[String::class.java] ?: error("No storage!")
        val p111 = s11.add(value = "v1")
        val s12 = testSuite.s1[Int::class.java] ?: error("No storage!")
        val p121 = s12.add(value = 421)
        //
        val s21 = testSuite.s2[String::class.java] ?: error("No storage!")
        val p211 = s21.add(value = "v2")
        val s22 = testSuite.s2[Int::class.java] ?: error("No storage!")
        val p221 = s22.add(value = 422)
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
        val s11 = testSuite.s1[String::class.java] ?: error("No storage!")
        val p111 = s11.add(value = "v1")
        val s12 = testSuite.s1[Int::class.java] ?: error("No storage!")
        val p121 = s12.add(value = 421)
        //
        val s21 = testSuite.s2[String::class.java] ?: error("No storage!")
        val p211 = s21.add(value = "v2")
        val s22 = testSuite.s2[Int::class.java] ?: error("No storage!")
        val p221 = s22.add(value = 422)
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
            assert = ::assertEquals,
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
            assert = ::assertEquals,
        )
    }

    @Test
    fun getMergeStatesTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.s1[String::class.java] ?: error("No storage!")
        val p111 = s11.add(value = "v1")
        val s12 = testSuite.s1[Int::class.java] ?: error("No storage!")
        val p121 = s12.add(value = 421)
        //
        val s21 = testSuite.s2[String::class.java] ?: error("No storage!")
        val p211 = s21.add(value = "v2")
        val s22 = testSuite.s2[Int::class.java] ?: error("No storage!")
        val p221 = s22.add(value = 422)
        //
        assertEquals(
            expected = mapOf(
                s11.id to mockMergeState(
                    downloaded = setOf(p111.valueInfo.id),
                    encoded = listOf(Transformers.Strings.map(payload = p211)),
                ),
                s12.id to mockMergeState(
                    downloaded = setOf(p121.valueInfo.id),
                    encoded = listOf(Transformers.Ints.map(payload = p221)),
                ),
            ),
            actual = testSuite.s2.getMergeStates(syncStates = testSuite.s1.getSyncStates()),
            assert = { expected, actual ->
                assertEquals(expected = expected.downloaded, actual = actual.downloaded, message = "downloaded")
                assertEquals(
                    expected = expected.encoded,
                    actual = actual.encoded,
                    comparator = Comparators.payloads,
                    assert = { index, e, a ->
                        assertEquals(e.valueInfo, a.valueInfo)
                        assertEquals(e.valueState, a.valueState)
                        assertTrue(e.value.contentEquals(a.value), "index: $index")
                    },
                )
                assertEquals(expected = expected.deleted, actual = actual.deleted, message = "deleted")
            },
        )
        //
        assertEquals(
            expected = mapOf(
                s21.id to mockMergeState(
                    downloaded = setOf(p211.valueInfo.id),
                    encoded = listOf(Transformers.Strings.map(payload = p111)),
                ),
                s22.id to mockMergeState(
                    downloaded = setOf(p221.valueInfo.id),
                    encoded = listOf(Transformers.Ints.map(payload = p121)),
                ),
            ),
            actual = testSuite.s1.getMergeStates(syncStates = testSuite.s2.getSyncStates()),
            assert = { expected, actual ->
                assertEquals(expected = expected.downloaded, actual = actual.downloaded, message = "downloaded")
                assertEquals(
                    expected = expected.encoded,
                    actual = actual.encoded,
                    comparator = Comparators.payloads,
                    assert = { index, e, a ->
                        assertEquals(e.valueInfo, a.valueInfo)
                        assertEquals(e.valueState, a.valueState)
                        assertTrue(e.value.contentEquals(a.value), "index: $index")
                    },
                )
                assertEquals(expected = expected.deleted, actual = actual.deleted, message = "deleted")
            },
        )
    }
}
