package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class CommitTest {
    @Test
    fun s1CommitTest(@TempDir dir: File) {
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
        val s2SyncStates = testSuite.s2.getSyncStates()
        val s1MergeStates = testSuite.s1.getMergeStates(s2SyncStates)
        val s2CommitStates = testSuite.s2.merge(s1MergeStates)
        assertEquals(expected = p111, actual = s11.payloads.single())
        assertEquals(expected = p121, actual = s12.payloads.single())
        assertEquals(
            expected = setOf(s11.id, s12.id),
            actual = testSuite.s1.commit(s2CommitStates),
        )
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
    }

    @Test
    fun s2CommitTest(@TempDir dir: File) {
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
        val s1SyncStates = testSuite.s1.getSyncStates()
        val s2MergeStates = testSuite.s2.getMergeStates(s1SyncStates)
        val s1CommitStates = testSuite.s1.merge(s2MergeStates)
        assertEquals(expected = p211, actual = s21.payloads.single())
        assertEquals(expected = p221, actual = s22.payloads.single())
        assertEquals(
            expected = setOf(s21.id, s22.id),
            actual = testSuite.s2.commit(s1CommitStates),
        )
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
    }

    @Test
    fun commitTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.storage<String>(1)
        val p111 = testSuite.add(1, value = "p111")
        val p112 = testSuite.add(1, value = "p112")
        val p113 = testSuite.add(1, value = "p113")
        val s12 = testSuite.storage<Int>(1)
        val p121 = testSuite.add(1, value = 41210)
        val p122 = testSuite.add(1, value = 41220)
        val p123 = testSuite.add(1, value = 41230)
        val s21 = testSuite.storage<String>(2)
        val p211 = testSuite.add(2, value = "p211")
        val p212 = testSuite.add(2, value = "p212")
        val p213 = testSuite.add(2, value = "p213")
        val s22 = testSuite.storage<Int>(2)
        val p221 = testSuite.add(2, value = 42210)
        val p222 = testSuite.add(2, value = 42220)
        val p223 = testSuite.add(2, value = 42230)
        //
        assertEquals(
            expected = listOf(p111, p112, p113),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122, p123),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p211, p212, p213),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p221, p222, p223),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        testSuite.s1.commit(testSuite.s2.merge(testSuite.s1.getMergeStates(testSuite.s2.getSyncStates())))
        assertEquals(
            expected = listOf(p111, p112, p113, p211, p212, p213),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122, p123, p221, p222, p223),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p111, p112, p113, p211, p212, p213),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122, p123, p221, p222, p223),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        //
        assertTrue(testSuite.delete<String>(1, p111.id))
        val u112 = testSuite.update(1, p112.id, "p111:updated") ?: TODO()
        val p114 = testSuite.add(1, "p114")
        assertTrue(testSuite.delete<String>(2, p211.id))
        val u212 = testSuite.update(1, p212.id, "p211:updated") ?: TODO()
        val p214 = testSuite.add(2, "p214")
        assertTrue(testSuite.delete<Int>(1, p121.id))
        val u122 = testSuite.update(1, p122.id, 41221) ?: TODO()
        val p124 = testSuite.add(1, 41240)
        assertTrue(testSuite.delete<Int>(2, p221.id))
        val u222 = testSuite.update(1, p222.id, 42221) ?: TODO()
        val p224 = testSuite.add(2, 42240)
        //
        testSuite.s1.commit(testSuite.s2.merge(testSuite.s1.getMergeStates(testSuite.s2.getSyncStates())))
        assertEquals(
            expected = listOf(u112, p113, p114, u212, p213, p214),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(u122, p123, p124, u222, p223, p224),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(u112, p113, p114, u212, p213, p214),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(u122, p123, p124, u222, p223, p224),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
    }
}
