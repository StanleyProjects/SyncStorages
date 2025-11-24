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
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val issuers = (0 until 2).map { _ ->
            testSuite.storages(builder = builder)
        }
        val strings = issuers.map { storages ->
            testSuite.add<String>(storages, count = 2, key = Keys.Strings)
        }
        val durations = issuers.map { storages ->
            testSuite.add<Duration>(storages, count = 2, key = Keys.Durations)
        }
        issuers[0].commit(issuers[1].merge(issuers[0].getMergeStates(issuers[1].getSyncStates())))
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, key = Keys.Strings),
                expected = issuers.indices.flatMap(strings::get),
            )
            testSuite.assertEquals(
                storage = testSuite.storage(storages, key = Keys.Durations),
                expected = issuers.indices.flatMap(durations::get),
            )
        }
        issuers.forEachIndexed { index, storages ->
            Keys.Strings.also { key ->
                val storage = testSuite.storage(storages, key = key)
                testSuite.delete(storage, strings[index][0].id)
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(storages, key = key)
                testSuite.delete(storage, durations[index][0].id)
            }
        }
        issuers.forEachIndexed { index, storages ->
            Keys.Strings.also { key ->
                val storage = testSuite.storage(storages, key = key)
                val deleted = listOf(strings[index][0]).map { it.id }.toSet()
                testSuite.assertEquals(
                    storage = storage,
                    expected = issuers.flatMapIndexed { i, _ -> strings[i] }.filter { !deleted.contains(it.id) },
                )
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(storages, key = key)
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
            Keys.Strings.also { key ->
                val storage = testSuite.storage(transmitter, key = key)
                expected.add(storage.id)
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(transmitter, key = key)
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
            Keys.Strings.also { key ->
                val expected = issuers.indices.flatMap(strings::get).toMutableList()
                expected.removeIf { payload -> strings.map { it[0].id }.contains(payload.id) }
                testSuite.assertEquals(
                    storage = testSuite.storage(storages, key = key),
                    expected = expected,
                )
            }
            Keys.Durations.also { key ->
                val expected = issuers.indices.flatMap(durations::get).toMutableList()
                expected.removeIf { payload -> durations.map { it[0].id }.contains(payload.id) }
                testSuite.assertEquals(
                    storage = testSuite.storage(storages, key = key),
                    expected = expected,
                )
            }
        }
        Keys.Strings.also { key ->
            testSuite.assertEquals(
                expected = testSuite.storage(issuers[0], key = key).payloads,
                actual = testSuite.storage(issuers[1], key = key).payloads,
                comparator = Comparators.payloads,
                assert = { _, e, a -> testSuite.assertEquals(expected = e, actual = a) },
            )
        }
        Keys.Durations.also { key ->
            testSuite.assertEquals(
                expected = testSuite.storage(issuers[0], key = key).payloads,
                actual = testSuite.storage(issuers[1], key = key).payloads,
                comparator = Comparators.payloads,
                assert = { _, e, a -> testSuite.assertEquals(expected = e, actual = a) },
            )
        }
    }

    @Test
    fun updateTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val issuers = (0 until 2).map { _ ->
            testSuite.storages(builder = builder)
        }
        val strings = issuers.map { storages ->
            testSuite.add<String>(storages, count = 2, key = Keys.Strings)
        }
        val durations = issuers.map { storages ->
            testSuite.add<Duration>(storages, count = 2, key = Keys.Durations)
        }
        issuers[0].commit(issuers[1].merge(issuers[0].getMergeStates(issuers[1].getSyncStates())))
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, key = Keys.Strings),
                expected = issuers.indices.flatMap(strings::get),
            )
            testSuite.assertEquals(
                storage = testSuite.storage(storages, key = Keys.Durations),
                expected = issuers.indices.flatMap(durations::get),
            )
        }
        (0 to 1).also { (r, t) ->
            val receiver = issuers[r]
            val transmitter = issuers[t]
            Keys.Strings.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val expected = strings[r].toMutableList()
                expected[0] = testSuite.update(storage, strings[r][0].id)
                testSuite.assertEquals(
                    storage = storage,
                    expected = expected + strings[t],
                )
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val expected = durations[r].toMutableList()
                expected[0] = testSuite.update(storage, durations[r][0].id)
                testSuite.assertEquals(
                    storage = storage,
                    expected = expected + durations[t],
                )
            }
            val expected = HashSet<UUID>()
            Keys.Strings.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                expected.add(storage.id)
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                expected.add(storage.id)
            }
            val syncStates = receiver.getSyncStates()
            val mergeStates = transmitter.getMergeStates(syncStates = syncStates)
            val commitStates = receiver.merge(mergeStates = mergeStates)
            testSuite.assertEquals(
                expected = expected,
                actual = transmitter.commit(commitStates = commitStates),
            )
            Keys.Strings.also { key ->
                val payloads = strings[r].toMutableList()
                payloads[0] = testSuite.payload(testSuite.storage(receiver, key = key), strings[r][0].id)
                payloads.addAll(strings[t])
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[r], key = key),
                    expected = payloads,
                )
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[r], key = key),
                    expected = testSuite.storage(issuers[t], key = key).payloads,
                )
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[t], key = key),
                    expected = testSuite.storage(issuers[r], key = key).payloads,
                )
            }
            Keys.Durations.also { key ->
                val payloads = durations[r].toMutableList()
                payloads[0] = testSuite.payload(testSuite.storage(receiver, key = key), durations[r][0].id)
                payloads.addAll(durations[t])
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[r], key = key),
                    expected = payloads,
                )
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[r], key = key),
                    expected = testSuite.storage(issuers[t], key = key).payloads,
                )
                testSuite.assertEquals(
                    storage = testSuite.storage(issuers[t], key = key),
                    expected = testSuite.storage(issuers[r], key = key).payloads,
                )
            }
        }
    }
}
