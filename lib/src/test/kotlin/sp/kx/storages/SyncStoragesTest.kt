package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.HexFormat
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStoragesTest {
    @Test
    fun getSyncStatesTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.s1[String::class.java] ?: error("No storage!")
        val p111 = s11.add(value = "v1")
        val s12 = testSuite.s1[Int::class.java] ?: error("No storage!")
        val p121 = s12.add(value = 421)
        //
        assertEquals(
            expected = mapOf(
                s11.id to mockSyncState(
                    valueStates = mapOf(
                        p111.valueInfo.id to mockValueState(
                            hash = HexFormat.of().parseHex("6654c734ccab8f440ff0825eb443dc7f"),
                            updated = 0.milliseconds,
                        ),
                    ),
                ),
                s12.id to mockSyncState(
                    valueStates = mapOf(
                        p121.valueInfo.id to mockValueState(
                            hash = HexFormat.of().parseHex("fcf80161e01fd2c25c8d3b5d9a67b27f"),
                            updated = 1.milliseconds,
                        ),
                    ),
                ),
            ),
            actual = testSuite.s1.getSyncStates(),
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
                ),
                s12.id to mockMergeState(
                    downloaded = setOf(p121.valueInfo.id),
                ),
            ),
            actual = testSuite.s2.getMergeStates(syncStates = testSuite.s1.getSyncStates()),
            assert = { expected, actual ->
                assertEquals(expected = expected.downloaded, actual = actual.downloaded)
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
                assertEquals(expected = expected.deleted, actual = actual.deleted)
            },
        )
    }
}
