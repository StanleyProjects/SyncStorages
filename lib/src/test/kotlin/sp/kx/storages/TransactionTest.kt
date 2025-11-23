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
            .add(Keys.Strings, "foo")
            .add(Keys.Strings, "bar")
            .add(Keys.Durations, 42.seconds)
            .add(Keys.Durations, 43.seconds)
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
                expected = listOf(42.seconds, 43.seconds),
                actual = values,
            )
        }
        val s0 = testSuite.storage(storages, key = Keys.Strings).payloads.firstOrNull { it.value == "bar" } ?: error("No payload!")
        val d0 = testSuite.storage(storages, key = Keys.Durations).payloads.firstOrNull { it.value == 43.seconds } ?: error("No payload!")
        transaction = Transaction.Builder()
            .add(Keys.Strings, "baz")
            .delete(Keys.Strings, id = s0.id)
            .add(Keys.Durations, 44.seconds)
            .delete(Keys.Durations, id = d0.id)
            .build()
        storages.commit(transaction = transaction)
        Keys.Strings.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf("foo", "baz"),
                actual = values,
            )
        }
        Keys.Durations.also { key ->
            val payloads = testSuite.storage(storages, key = key).payloads
            val values = payloads.map { it.value }
            testSuite.assertEquals(
                expected = listOf(42.seconds, 44.seconds),
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
