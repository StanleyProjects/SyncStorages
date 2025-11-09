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
                expected = issuers.indices.flatMap(strings::get),
            )
            testSuite.assertEquals(
                storage = testSuite.storage(storages, Duration::class.java),
                expected = issuers.indices.flatMap(durations::get),
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
        issuers.forEach { storages ->
            String::class.java.also { type ->
                val expected = issuers.indices.flatMap(strings::get).toMutableList()
                expected.removeIf { payload -> strings.map { it[0].id }.contains(payload.id) }
                testSuite.assertEquals(
                    storage = testSuite.storage(storages, type),
                    expected = expected,
                )
            }
            Duration::class.java.also { type ->
                val expected = issuers.indices.flatMap(durations::get).toMutableList()
                expected.removeIf { payload -> durations.map { it[0].id }.contains(payload.id) }
                testSuite.assertEquals(
                    storage = testSuite.storage(storages, type),
                    expected = expected,
                )
            }
        }
        String::class.java.also { type ->
            testSuite.assertEquals(
                expected = testSuite.storage(issuers[0], type).payloads,
                actual = testSuite.storage(issuers[1], type).payloads,
                comparator = Comparators.payloads,
                assert = { _, e, a -> testSuite.assertEquals(expected = e, actual = a) },
            )
        }
        Duration::class.java.also { type ->
            testSuite.assertEquals(
                expected = testSuite.storage(issuers[0], type).payloads,
                actual = testSuite.storage(issuers[1], type).payloads,
                comparator = Comparators.payloads,
                assert = { _, e, a -> testSuite.assertEquals(expected = e, actual = a) },
            )
        }
    }

    @Test
    fun updateTest(@TempDir dir: File) {
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
                expected = issuers.indices.flatMap(strings::get),
            )
            testSuite.assertEquals(
                storage = testSuite.storage(storages, Duration::class.java),
                expected = issuers.indices.flatMap(durations::get),
            )
        }
        (0 to 1).also { (r, t) ->
            val receiver = issuers[r]
            val transmitter = issuers[t]
            String::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                val expected = strings[r].toMutableList()
                expected[0] = testSuite.update(storage, strings[r][0].id)
                testSuite.assertEquals(
                    storage = storage,
                    expected = expected + strings[t],
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                val expected = durations[r].toMutableList()
                expected[0] = testSuite.update(storage, durations[r][0].id)
                testSuite.assertEquals(
                    storage = storage,
                    expected = expected + durations[t],
                )
            }
            val expected = HashSet<UUID>()
            String::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                expected.add(storage.id)
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                expected.add(storage.id)
            }
            val syncStates = receiver.getSyncStates()
            val mergeStates = transmitter.getMergeStates(syncStates = syncStates)
            val commitStates = receiver.merge(mergeStates = mergeStates)
            testSuite.assertEquals(
                expected = expected,
                actual = transmitter.commit(commitStates = commitStates),
            )
            String::class.java.also { type ->
                val payloads = strings[r].toMutableList()
                payloads[0] = testSuite.payload(testSuite.storage(receiver, type), strings[r][0].id)
                payloads.addAll(strings[t])
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[r], type),
                    expected = payloads,
                )
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[r], type),
                    expected = testSuite.storage(issuers[t], type).payloads,
                )
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[t], type),
                    expected = testSuite.storage(issuers[r], type).payloads,
                )
            }
            Duration::class.java.also { type ->
                val payloads = durations[r].toMutableList()
                payloads[0] = testSuite.payload(testSuite.storage(receiver, type), durations[r][0].id)
                payloads.addAll(durations[t])
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[r], type),
                    expected = payloads,
                )
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[r], type),
                    expected = testSuite.storage(issuers[t], type).payloads,
                )
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[t], type),
                    expected = testSuite.storage(issuers[r], type).payloads,
                )
            }
        }
    }
}
