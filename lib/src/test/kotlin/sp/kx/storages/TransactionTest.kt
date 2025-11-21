package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal class TransactionTest {
    @Test
    fun addTest(@TempDir dir: File) {
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
}
