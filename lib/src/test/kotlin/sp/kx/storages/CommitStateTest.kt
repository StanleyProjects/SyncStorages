package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.HexFormat

internal class CommitStateTest {
    @Test
    fun s1MergeTest(@TempDir dir: File) {
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
        val s2MergeStates = testSuite.s2.getMergeStates(testSuite.s1.getSyncStates())
        assertEquals(
            expected = mapOf(
                s11.id to mockCommitState(
                    gives = listOf(Transformers.Strings.map(p111)),
                    hash = HexFormat.of().parseHex("ad8a19c4cc9baadc107b4e397cb0fdc3"),
                ),
                s12.id to mockCommitState(
                    gives = listOf(Transformers.Ints.map(p121)),
                    hash = HexFormat.of().parseHex("6aaa777a09f161ae17bea22665cf314c"),
                ),
            ),
            actual = testSuite.s1.merge(mergeStates = s2MergeStates),
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
    }

    @Test
    fun s2MergeTest(@TempDir dir: File) {
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
        val s1MergeStates = testSuite.s1.getMergeStates(testSuite.s2.getSyncStates())
        assertEquals(
            expected = mapOf(
                s21.id to mockCommitState(
                    gives = listOf(Transformers.Strings.map(p211)),
                    hash = HexFormat.of().parseHex("ad8a19c4cc9baadc107b4e397cb0fdc3"),
                ),
                s22.id to mockCommitState(
                    gives = listOf(Transformers.Ints.map(p221)),
                    hash = HexFormat.of().parseHex("6aaa777a09f161ae17bea22665cf314c"),
                ),
            ),
            actual = testSuite.s2.merge(mergeStates = s1MergeStates),
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
    }
}
