package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.HashSet
import java.util.UUID
import kotlin.time.Duration

internal class CommitTest {
    @Test
    fun deleteTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(UUID(0, 0), String::class.java, Transformers.Strings)
            .add(UUID(1, 0), Duration::class.java, Transformers.Durations)
        val issuers = (0 until 2).map { _ ->
            testSuite.storages(builder = builder)
        }
        val strings = issuers.map { storages ->
            testSuite.add<String>(storages, count = 2)
        }
        val durations = issuers.map { storages ->
            testSuite.add<Duration>(storages, count = 2)
        }
        issuers[0].commit(issuers[1].merge(issuers[0].getMergeStates(issuers[1].getSyncStates())))
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, String::class.java),
                expected = issuers.flatMapIndexed { index, _ -> strings[index] },
            )
        }
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, Duration::class.java),
                expected = issuers.flatMapIndexed { index, _ -> durations[index] },
            )
        }
        issuers.forEachIndexed { index, storages ->
            String::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                testSuite.delete(storage, strings[index][0].id)
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                testSuite.delete(storage, durations[index][0].id)
            }
        }
        issuers.forEachIndexed { index, storages ->
            String::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                val deleted = listOf(strings[index][0]).map { it.id }.toSet()
                testSuite.assertEquals(
                    storage = storage,
                    expected = issuers.flatMapIndexed { i, _ -> strings[i] }.filter { !deleted.contains(it.id) },
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                val deleted = listOf(durations[index][0]).map { it.id }.toSet()
                testSuite.assertEquals(
                    storage = storage,
                    expected = issuers.flatMapIndexed { i, _ -> durations[i] }.filter { !deleted.contains(it.id) },
                )
            }
        }
        (0 to 1).also { (r, t) ->
            val receiver = issuers[r]
            val transmitter = issuers[t]
            val expected = HashSet<UUID>()
            String::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                expected.add(storage.id)
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                expected.add(storage.id)
            }
            val syncStates = receiver.getSyncStates()
            val mergeStates = transmitter.getMergeStates(syncStates = syncStates)
            val commitStates = receiver.merge(mergeStates = mergeStates)
            testSuite.assertEquals(
                expected = expected,
                actual = transmitter.commit(commitStates = commitStates),
            )
        }
    }
}
