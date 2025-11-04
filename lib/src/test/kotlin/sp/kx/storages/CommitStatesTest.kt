package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.Serializable
import java.util.HexFormat

internal class CommitStatesTest {
    @Test
    fun addTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        setOf(String::class.java, Int::class.java).forEach { type ->
            testSuite.add(index = 0, type = type, count = 2)
            testSuite.add(index = 1, type = type, count = 2)
        }
        val p000 = testSuite.payload<String>(0, 0)
        val p001 = testSuite.payload<String>(0, 1)
        val p010 = testSuite.payload<Int>(0, 0)
        val p011 = testSuite.payload<Int>(0, 1)
        //
        val p100 = testSuite.payload<String>(1, 0)
        val p101 = testSuite.payload<String>(1, 1)
        val p110 = testSuite.payload<Int>(1, 0)
        val p111 = testSuite.payload<Int>(1, 1)
        //
        testSuite.assertEquals(0, listOf(p000, p001))
        testSuite.assertEquals(0, listOf(p010, p011))
        testSuite.assertEquals(1, listOf(p100, p101))
        testSuite.assertEquals(1, listOf(p110, p111))
        //
        val s0SyncStates = testSuite.storages(0).getSyncStates()
        val s1MergeStates = testSuite.storages(1).getMergeStates(s0SyncStates)
        testSuite.merge(
            index = 0,
            mergeStates = s1MergeStates,
            expected = mapOf(
                testSuite.storage<String>(0).id to testSuite.commitState(
                    gives = listOf(p000, p001),
                    expected = listOf(p000, p001, p100, p101),
                    transform = Transformers.Strings::map,
                ),
                testSuite.storage<Int>(0).id to testSuite.commitState(
                    gives = listOf(p010, p011),
                    expected = listOf(p010, p011, p110, p111),
                    transform = Transformers.Ints::map,
                ),
            ),
        )
        //
        testSuite.assertEquals(0, listOf(p000, p001, p100, p101))
        testSuite.assertEquals(0, listOf(p010, p011, p110, p111))
        testSuite.assertEquals(1, listOf(p100, p101))
        testSuite.assertEquals(1, listOf(p110, p111))
    }

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
