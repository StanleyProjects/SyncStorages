package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.HexFormat

internal class CommitStatesTest {
    @Test
    fun s1MergeTest(@TempDir dir: File) {
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
        val s2MergeStates = testSuite.s2.getMergeStates(testSuite.s1.getSyncStates())
        assertEquals(expected = p111, actual = s11.payloads.single())
        assertEquals(expected = p121, actual = s12.payloads.single())
        val s1CommitStates = testSuite.s1.merge(mergeStates = s2MergeStates)
        assertEquals(
            expected = listOf(p111, p211),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p221),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        //
        assertEquals(
            expected = mapOf(
                s11.id to mockCommitState(
                    gives = listOf(Transformers.Strings.map(p111)),
                    hash = testSuite.hashOf(payloads = listOf(p111, p211).map(Transformers.Strings::map)),
                ),
                s12.id to mockCommitState(
                    gives = listOf(Transformers.Ints.map(p121)),
                    hash = testSuite.hashOf(payloads = listOf(p121, p221).map(Transformers.Ints::map)),
                ),
            ),
            actual = s1CommitStates,
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
    }

    @Test
    fun s2MergeTest(@TempDir dir: File) {
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
        val s1MergeStates = testSuite.s1.getMergeStates(testSuite.s2.getSyncStates())
        assertEquals(expected = p211, actual = s21.payloads.single())
        assertEquals(expected = p221, actual = s22.payloads.single())
        val s2CommitStates = testSuite.s2.merge(mergeStates = s1MergeStates)
        assertEquals(
            expected = listOf(p111, p211),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p221),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        //
        assertEquals(
            expected = mapOf(
                s21.id to mockCommitState(
                    gives = listOf(Transformers.Strings.map(p211)),
                    hash = testSuite.hashOf(payloads = listOf(p111, p211).map(Transformers.Strings::map)),
                ),
                s22.id to mockCommitState(
                    gives = listOf(Transformers.Ints.map(p221)),
                    hash = testSuite.hashOf(payloads = listOf(p121, p221).map(Transformers.Ints::map)),
                ),
            ),
            actual = s2CommitStates,
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
    }
}
