package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import sp.kx.bytes.hex
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
    fun deleteTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        //
        val s11 = testSuite.storage<String>(1)
        val p111 = testSuite.add(1, value = "p111")
        val p112 = testSuite.add(1, value = "p112")
        val s12 = testSuite.storage<Int>(1)
        val p121 = testSuite.add(1, value = 41210)
        val p122 = testSuite.add(1, value = 41220)
        val s21 = testSuite.storage<String>(2)
        val p211 = testSuite.add(2, value = "p211")
        val p212 = testSuite.add(2, value = "p212")
        val s22 = testSuite.storage<Int>(2)
        val p221 = testSuite.add(2, value = 42210)
        val p222 = testSuite.add(2, value = 42220)
        //
        assertEquals(
            expected = listOf(p111, p112),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p211, p212),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p221, p222),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = setOf(s11.id, s12.id),
            actual = testSuite.s1.commit(
                commitStates = testSuite.s2.merge(
                    mergeStates = testSuite.s1.getMergeStates(testSuite.s2.getSyncStates()),
                ),
            ),
        )
        //
        assertEquals(
            expected = listOf(p111, p112, p211, p212),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122, p221, p222),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p111, p112, p211, p212),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122, p221, p222),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        //
        assertTrue(testSuite.delete<String>(1, p111.id))
        assertTrue(testSuite.delete<String>(2, p211.id))
        assertTrue(testSuite.delete<Int>(1, p121.id))
        assertTrue(testSuite.delete<Int>(2, p221.id))
        //
        val s2CommitStates = testSuite.s2.merge(testSuite.s1.getMergeStates(testSuite.s2.getSyncStates()))
        assertEquals(
            expected = mapOf(
                s21.id to mockCommitState(
                    deleted = setOf(p111.id, p211.id),
                    hash = testSuite.hashOf(payloads = listOf(p112, p212).map(Transformers.Strings::map)),
                ),
                s22.id to mockCommitState(
                    deleted = setOf(p121.id, p221.id),
                    hash = testSuite.hashOf(payloads = listOf(p122, p222).map(Transformers.Ints::map)),
                ),
            ),
            actual = s2CommitStates,
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
        assertEquals(expected = setOf(s21.id, s22.id), actual = testSuite.s1.commit(s2CommitStates))
        assertEquals(
            expected = listOf(p112, p212),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p122, p222),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p112, p212),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p122, p222),
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
        val p114 = testSuite.add(1, value = "p114")
        val p115 = testSuite.add(1, value = "p115")
        val s12 = testSuite.storage<Int>(1)
        val p121 = testSuite.add(1, value = 41210)
        val p122 = testSuite.add(1, value = 41220)
        val p123 = testSuite.add(1, value = 41230)
        val p124 = testSuite.add(1, value = 41240)
        val p125 = testSuite.add(1, value = 41250)
        val s21 = testSuite.storage<String>(2)
        val p211 = testSuite.add(2, value = "p211")
        val p212 = testSuite.add(2, value = "p212")
        val p213 = testSuite.add(2, value = "p213")
        val p214 = testSuite.add(2, value = "p214")
        val p215 = testSuite.add(2, value = "p215")
        val s22 = testSuite.storage<Int>(2)
        val p221 = testSuite.add(2, value = 42210)
        val p222 = testSuite.add(2, value = 42220)
        val p223 = testSuite.add(2, value = 42230)
        val p224 = testSuite.add(2, value = 42240)
        val p225 = testSuite.add(2, value = 42250)
        //
        assertEquals(
            expected = listOf(p111, p112, p113, p114, p115),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122, p123, p124, p125),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p211, p212, p213, p214, p215),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p221, p222, p223, p224, p225),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = setOf(s11.id, s12.id),
            actual = testSuite.s1.commit(
                commitStates = testSuite.s2.merge(
                    mergeStates = testSuite.s1.getMergeStates(testSuite.s2.getSyncStates()),
                ),
            ),
        )
        assertEquals(
            expected = listOf(p111, p112, p113, p114, p115, p211, p212, p213, p214, p215),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122, p123, p124, p125, p221, p222, p223, p224, p225),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p111, p112, p113, p114, p115, p211, p212, p213, p214, p215),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(p121, p122, p123, p124, p125, p221, p222, p223, p224, p225),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        //
        assertTrue(testSuite.delete<String>(1, p111.id))
        val u112 = testSuite.update(1, p112.id, "p111:updated") ?: TODO()
        val u114 = testSuite.update(1, p114.id, "p114:updated") ?: TODO()
        val p119 = testSuite.add(1, "p119")
        assertTrue(testSuite.delete<String>(2, p211.id))
        val u212 = testSuite.update(1, p212.id, "p211:updated") ?: TODO()
        val u214 = testSuite.update(1, p214.id, "p214:updated") ?: TODO()
        val p219 = testSuite.add(1, "p219")
        assertTrue(testSuite.delete<Int>(2, p121.id))
        val u122 = testSuite.update(2, p122.id, 41221) ?: TODO()
        val u124 = testSuite.update(2, p124.id, 41241) ?: TODO()
        val p129 = testSuite.add(2, 41290)
        assertTrue(testSuite.delete<Int>(2, p221.id))
        val u222 = testSuite.update(2, p222.id, 42221) ?: TODO()
        val u224 = testSuite.update(2, p224.id, 42241) ?: TODO()
        val p229 = testSuite.add(2, 42290)
        assertTrue(testSuite.delete<String>(1, p214.id))
        assertTrue(testSuite.delete<String>(2, p114.id))
        assertTrue(testSuite.delete<Int>(1, p224.id))
        assertTrue(testSuite.delete<Int>(2, p124.id))
        val u115 = testSuite.update(1, p115.id, "p115:u1") ?: TODO()
        val uuu2 = testSuite.update(1, p215.id, "p215:u1") ?: TODO()
        val u215 = testSuite.update(2, p215.id, "p215:u2") ?: TODO()
        val uuu1 = testSuite.update(1, p125.id, 412511) ?: TODO()
        val u225 = testSuite.update(2, p225.id, 422512) ?: TODO()
        val u125 = testSuite.update(2, p125.id, 412512) ?: TODO()
        //
        val s2CommitStates = testSuite.s2.merge(testSuite.s1.getMergeStates(testSuite.s2.getSyncStates()))
        assertEquals(
            expected = mapOf(
                s21.id to mockCommitState(
                    deleted = setOf(p111.id, p114.id, p211.id, p214.id),
                    gives = listOf(p219).map(Transformers.Strings::map),
                    hash = testSuite.hashOf(payloads = listOf(u112, p113, p119, u212, p213, p219).map(Transformers.Strings::map)),
                ),
                s22.id to mockCommitState(
                    deleted = setOf(p121.id, p124.id, p221.id, p224.id),
                    gives = listOf(p229).map(Transformers.Ints::map),
                    hash = testSuite.hashOf(payloads = listOf(u122, p123, p129, u222, p223, p229).map(Transformers.Ints::map)),
                ),
            ),
            actual = s2CommitStates,
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
        assertEquals(expected = setOf(s21.id, s22.id), actual = testSuite.s1.commit(s2CommitStates))
        assertEquals(
            expected = listOf(u112, p113, p119, u212, p213, p219),
            actual = s11.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(u122, p123, p129, u222, p223, p229),
            actual = s12.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(u112, p113, p119, u212, p213, p219),
            actual = s21.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
        assertEquals(
            expected = listOf(u122, p123, p129, u222, p223, p229),
            actual = s22.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
    }
}
