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
            .add(UUID(0, 0), String::class.java, Transformers.Strings)
            .add(UUID(1, 0), Duration::class.java, Transformers.Durations)
        val issuers = (0 until 2).map { _ ->
            testSuite.storages(builder = builder)
        }
        issuers.forEach { storages ->
            testSuite.add<String>(storages, count = 2)
            testSuite.add<Duration>(storages, count = 2)
        }
        issuers[0].also { receiver ->
            val transmitter = issuers[1]
            val expected = mutableMapOf<UUID, CommitState>()
            String::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                val payloads = storage.payloads + testSuite.storage(transmitter, type).payloads
                expected[storage.id] = mockCommitState(
                    gives = storage.payloads.map(Transformers::map),
                    hash = testSuite.hashOf(payloads = payloads),
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                val payloads = storage.payloads + testSuite.storage(transmitter, type).payloads
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
            String::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                val payloads = testSuite.storage(transmitter, type).payloads
                expected[storage.id] = mockCommitState(
                    hash = testSuite.hashOf(payloads = payloads),
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                val payloads = testSuite.storage(transmitter, type).payloads
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
            val expected = mutableMapOf<UUID, CommitState>()
            String::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                val deleted = setOf(strings[r][0].id, strings[t][0].id)
                val payloads = issuers.flatMapIndexed { i, _ -> strings[i] }.filter { !deleted.contains(it.id) }
                expected[storage.id] = mockCommitState(
                    deleted = deleted,
                    hash = testSuite.hashOf(payloads),
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
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
}
