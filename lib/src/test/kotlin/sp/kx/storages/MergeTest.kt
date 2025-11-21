package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import kotlin.time.Duration

internal class MergeTest {
    @Test
    fun mergeTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
            .add(Keys.Durations, Transformers.Durations)
        val issuers = (0 until 2).map { _ ->
            testSuite.storages(builder = builder)
        }
        issuers.forEach { storages ->
            testSuite.add(storages, count = 2, key = Keys.Strings)
            testSuite.add(storages, count = 2, key = Keys.Durations)
        }
        issuers[0].also { receiver ->
            val transmitter = issuers[1]
            val expected = mutableMapOf<UUID, CommitState>()
            Keys.Strings.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val payloads = storage.payloads + testSuite.storage(transmitter, key = key).payloads
                expected[storage.id] = mockCommitState(
                    gives = storage.payloads.map(Transformers::map),
                    hash = testSuite.hashOf(payloads = payloads),
                )
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val payloads = storage.payloads + testSuite.storage(transmitter, key = key).payloads
                expected[storage.id] = mockCommitState(
                    gives = storage.payloads.map(Transformers::map),
                    hash = testSuite.hashOf(payloads = payloads),
                )
            }
            val mergeStates = transmitter.getMergeStates(syncStates = receiver.getSyncStates())
            testSuite.assertEquals(
                expected = expected,
                actual = receiver.merge(mergeStates = mergeStates),
                assert = testSuite::assertEquals,
            )
        }
        issuers[1].also { receiver ->
            val transmitter = issuers[0]
            val expected = mutableMapOf<UUID, CommitState>()
            Keys.Strings.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val payloads = testSuite.storage(transmitter, key = key).payloads
                expected[storage.id] = mockCommitState(
                    hash = testSuite.hashOf(payloads = payloads),
                )
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val payloads = testSuite.storage(transmitter, key = key).payloads
                expected[storage.id] = mockCommitState(
                    hash = testSuite.hashOf(payloads = payloads),
                )
            }
            val mergeStates = transmitter.getMergeStates(syncStates = receiver.getSyncStates())
            testSuite.assertEquals(
                expected = expected,
                actual = receiver.merge(mergeStates = mergeStates),
                assert = testSuite::assertEquals,
            )
        }
    }

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
            testSuite.add(storages, count = 2, key = Keys.Strings)
        }
        val durations = issuers.map { storages ->
            testSuite.add(storages, count = 2, key = Keys.Durations)
        }
        issuers[0].commit(issuers[1].merge(issuers[0].getMergeStates(issuers[1].getSyncStates())))
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, key = Keys.Strings),
                expected = issuers.flatMapIndexed { index, _ -> strings[index] },
            )
        }
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, Keys.Durations),
                expected = issuers.flatMapIndexed { index, _ -> durations[index] },
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
            val expected = mutableMapOf<UUID, CommitState>()
            Keys.Strings.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val deleted = setOf(strings[r][0].id, strings[t][0].id)
                val payloads = issuers.flatMapIndexed { i, _ -> strings[i] }.filter { !deleted.contains(it.id) }
                expected[storage.id] = mockCommitState(
                    deleted = deleted,
                    hash = testSuite.hashOf(payloads),
                )
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val deleted = setOf(durations[r][0].id, durations[t][0].id)
                val payloads = issuers.flatMapIndexed { i, _ -> durations[i] }.filter { !deleted.contains(it.id) }
                expected[storage.id] = mockCommitState(
                    deleted = deleted,
                    hash = testSuite.hashOf(payloads),
                )
            }
            val mergeStates = transmitter.getMergeStates(syncStates = receiver.getSyncStates())
            testSuite.assertEquals(
                expected = expected,
                actual = receiver.merge(mergeStates = mergeStates),
                assert = testSuite::assertEquals,
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
            testSuite.add(storages, count = 2, key = Keys.Strings)
        }
        val durations = issuers.map { storages ->
            testSuite.add(storages, count = 2, key = Keys.Durations)
        }
        issuers[0].commit(issuers[1].merge(issuers[0].getMergeStates(issuers[1].getSyncStates())))
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, key = Keys.Strings),
                expected = issuers.flatMapIndexed { index, _ -> strings[index] },
            )
        }
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, Keys.Durations),
                expected = issuers.flatMapIndexed { index, _ -> durations[index] },
            )
        }
        (0 to 1).also { (r, t) ->
            val receiver = issuers[r]
            val transmitter = issuers[t]
            Keys.Strings.also { key ->
                val storage = testSuite.storage(transmitter, key = key)
                val expected = strings[t].toMutableList()
                expected[0] = testSuite.update(storage, strings[t][0].id)
                testSuite.assertEquals(
                    storage = storage,
                    expected = expected + strings[r],
                )
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(transmitter, key = key)
                val expected = durations[t].toMutableList()
                expected[0] = testSuite.update(storage, durations[t][0].id)
                testSuite.assertEquals(
                    storage = storage,
                    expected = expected + durations[r],
                )
            }
            val expected = mutableMapOf<UUID, CommitState>()
            Keys.Strings.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val payloads = strings[t].toMutableList()
                payloads[0] = testSuite.payload(testSuite.storage(transmitter, key = key), strings[t][0].id)
                payloads.addAll(strings[r])
                expected[storage.id] = mockCommitState(
                    hash = testSuite.hashOf(payloads),
                )
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(receiver, key = key)
                val payloads = durations[t].toMutableList()
                payloads[0] = testSuite.payload(testSuite.storage(transmitter, key = key), durations[t][0].id)
                payloads.addAll(durations[r])
                expected[storage.id] = mockCommitState(
                    hash = testSuite.hashOf(payloads),
                )
            }
            val mergeStates = transmitter.getMergeStates(syncStates = receiver.getSyncStates())
            testSuite.assertEquals(
                expected = expected,
                actual = receiver.merge(mergeStates = mergeStates),
                assert = testSuite::assertEquals,
            )
        }
    }
}
