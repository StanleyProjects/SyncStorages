package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal class TransactionTest {
    @Test
    fun defaultTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val storages = testSuite.storages(builder = builder)
        testSuite.assertEquals(
            storage = testSuite.storage(storages, key = Keys.Strings),
            expected = emptyList(),
        )
        testSuite.assertEquals(
            storage = testSuite.storage(storages, key = Keys.Durations),
            expected = emptyList(),
        )
        var transaction = Transaction.Builder()
            .add(Keys.Strings, "s00")
            .add(Keys.Strings, "s01")
            .add(Keys.Strings, "s02")
            .add(Keys.Strings, "s03")
            .add(Keys.Durations, 100.seconds)
            .add(Keys.Durations, 101.seconds)
            .add(Keys.Durations, 102.seconds)
            .add(Keys.Durations, 103.seconds)
            .build()
        storages.commit(transaction = transaction)
        Keys.Strings.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf("s00", "s01", "s02", "s03"),
                actual = values,
            )
        }
        Keys.Durations.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf(100.seconds, 101.seconds, 102.seconds, 103.seconds),
                actual = values,
            )
        }
        val s0 = testSuite.payload(storages, key = Keys.Strings) { it.value == "s00" }
        val s1 = testSuite.payload(storages, key = Keys.Strings) { it.value == "s01" }
        val d0 = testSuite.payload(storages, key = Keys.Durations) { it.value == 100.seconds }
        val d1 = testSuite.payload(storages, key = Keys.Durations) { it.value == 101.seconds }
        transaction = Transaction.Builder()
            .delete(Keys.Strings, id = s0.id)
            .update(Keys.Strings, id = s1.id, value = "s01:updated")
            .deleteFirst(Keys.Strings) { it.value == "s03" }
            .add(Keys.Strings, "s09")
            .delete(Keys.Durations, id = d0.id)
            .update(Keys.Durations, id = d1.id, value = 111.seconds)
            .deleteFirst(Keys.Durations) { it.value == 103.seconds }
            .add(Keys.Durations, 109.seconds)
            .build()
        storages.commit(transaction = transaction)
        Keys.Strings.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf("s01:updated", "s02", "s09"),
                actual = values,
            )
        }
        Keys.Durations.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf(111.seconds, 102.seconds, 109.seconds),
                actual = values,
            )
        }
    }

    @Test
    fun addTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val storages = testSuite.storages(builder = builder)
        val transaction = Transaction.Builder()
            .add(Keys.Strings, "foo")
            .add(Keys.Strings, "bar")
            .add(Keys.Durations, 42.seconds)
            .build()
        storages.commit(transaction = transaction)
        Keys.Strings.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf("foo", "bar"),
                actual = values,
            )
        }
        Keys.Durations.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf(42.seconds),
                actual = values,
            )
        }
    }

    @Test
    fun deleteTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val storages = testSuite.storages(builder = builder)
        var transaction = Transaction.Builder()
            .add(Keys.Strings, "foo")
            .add(Keys.Durations, 42.seconds)
            .build()
        storages.commit(transaction = transaction)
        val s0 = testSuite.storage(storages, key = Keys.Strings).payloads.single()
        val d0 = testSuite.storage(storages, key = Keys.Durations).payloads.single()
        transaction = Transaction.Builder()
            .delete(key = Keys.Strings, id = s0.id)
            .delete(key = Keys.Durations, id = d0.id)
            .build()
        storages.commit(transaction = transaction)
        testSuite.assertEquals(
            storage = testSuite.storage(storages, key = Keys.Strings),
            expected = emptyList(),
        )
        testSuite.assertEquals(
            storage = testSuite.storage(storages, key = Keys.Durations),
            expected = emptyList(),
        )
    }

    @Test
    fun deleteFirstTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val storages = testSuite.storages(builder = builder)
        var transaction = Transaction.Builder()
            .add(Keys.Strings, "foo")
            .add(Keys.Durations, 42.seconds)
            .build()
        storages.commit(transaction = transaction)
        transaction = Transaction.Builder()
            .deleteFirst(key = Keys.Strings) { it.value == "foo" }
            .deleteFirst(key = Keys.Durations) { it.value == 42.seconds }
            .build()
        storages.commit(transaction = transaction)
        testSuite.assertEquals(
            storage = testSuite.storage(storages, key = Keys.Strings),
            expected = emptyList(),
        )
        testSuite.assertEquals(
            storage = testSuite.storage(storages, key = Keys.Durations),
            expected = emptyList(),
        )
    }

    @Test
    fun deleteAllTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val storages = testSuite.storages(builder = builder)
        testSuite.assertEquals(
            storage = testSuite.storage(storages, key = Keys.Strings),
            expected = emptyList(),
        )
        testSuite.assertEquals(
            storage = testSuite.storage(storages, key = Keys.Durations),
            expected = emptyList(),
        )
        var transaction = Transaction.Builder()
            .add(Keys.Strings, "s00")
            .add(Keys.Strings, "s01")
            .add(Keys.Strings, "s02")
            .add(Keys.Strings, "s10")
            .add(Keys.Durations, 100.seconds)
            .add(Keys.Durations, 101.seconds)
            .add(Keys.Durations, 102.seconds)
            .add(Keys.Durations, 110.seconds)
            .build()
        storages.commit(transaction = transaction)
        check(testSuite.storage(storages, key = Keys.Strings).payloads.size == 4)
        check(testSuite.storage(storages, key = Keys.Durations).payloads.size == 4)
        transaction = Transaction.Builder()
            .deleteAll(Keys.Strings) { it.value.startsWith("s0") }
            .deleteAll(Keys.Durations) { it.value.inWholeSeconds < 110 }
            .build()
        storages.commit(transaction = transaction)
        check(testSuite.storage(storages, key = Keys.Strings).payloads.single().value == "s10")
        check(testSuite.storage(storages, key = Keys.Durations).payloads.single().value == 110.seconds)
    }

    @Test
    fun updateTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val storages = testSuite.storages(builder = builder)
        var transaction = Transaction.Builder()
            .add(Keys.Strings, "foo")
            .add(Keys.Durations, 42.seconds)
            .build()
        storages.commit(transaction = transaction)
        val s0 = testSuite.storage(storages, key = Keys.Strings).payloads.single()
        val d0 = testSuite.storage(storages, key = Keys.Durations).payloads.single()
        transaction = Transaction.Builder()
            .update(key = Keys.Strings, id = s0.id, value = "foo:updated")
            .update(key = Keys.Durations, id = d0.id, value = 142.seconds)
            .build()
        storages.commit(transaction = transaction)
        Keys.Strings.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf("foo:updated"),
                actual = values,
            )
        }
        Keys.Durations.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf(142.seconds),
                actual = values,
            )
        }
    }
}
