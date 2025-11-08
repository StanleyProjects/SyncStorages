package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import kotlin.time.Duration

internal class MergeStatesTest {
    @Test
    fun getMergeStatesTest(@TempDir dir: File) {
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
        issuers.forEachIndexed { index, storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, String::class.java),
                expected = strings[index],
            )
        }
        issuers.forEachIndexed { index, storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, Duration::class.java),
                expected = durations[index],
            )
        }
        listOf(0 to 1, 1 to 0).forEach { (r, t) ->
            val receiver = issuers[r]
            val transmitter = issuers[t]
            val expected = mutableMapOf<UUID, MergeState>()
            String::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                expected[storage.id] = mockMergeState(
                    picks = strings[r].map { it.id }.toSet(),
                    gives = strings[t].map(Transformers::map),
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                expected[storage.id] = mockMergeState(
                    picks = durations[r].map { it.id }.toSet(),
                    gives = durations[t].map(Transformers::map),
                )
            }
            testSuite.assertEquals(
                expected = expected,
                actual = transmitter.getMergeStates(syncStates = receiver.getSyncStates()),
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
        listOf(0 to 1, 1 to 0).forEach { (r, t) ->
            val receiver = issuers[r]
            val transmitter = issuers[t]
            val expected = mutableMapOf<UUID, MergeState>()
            String::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                val deleted = listOf(strings[t][0]).map { it.id }.toSet()
                expected[storage.id] = mockMergeState(deleted = deleted)
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                val deleted = listOf(durations[t][0]).map { it.id }.toSet()
                expected[storage.id] = mockMergeState(deleted = deleted)
            }
            testSuite.assertEquals(
                expected = expected,
                actual = transmitter.getMergeStates(syncStates = receiver.getSyncStates()),
                assert = testSuite::assertEquals,
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
                expected = issuers.flatMapIndexed { index, _ -> strings[index] },
            )
        }
        issuers.forEach { storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, Duration::class.java),
                expected = issuers.flatMapIndexed { index, _ -> durations[index] },
            )
        }
        issuers[0].also { storages ->
            String::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                val expected = strings[0].toMutableList()
                expected[0] = testSuite.update(storage, strings[0][0].id)
                testSuite.assertEquals(
                    storage = storage,
                    expected = expected + strings[1],
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                val expected = durations[0].toMutableList()
                expected[0] = testSuite.update(storage, durations[0][0].id)
                testSuite.assertEquals(
                    storage = storage,
                    expected = expected + durations[1],
                )
            }
        }
        (0 to 1).also { (r, t) ->
            val receiver = issuers[r]
            val transmitter = issuers[t]
            val expected = mutableMapOf<UUID, MergeState>()
            String::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                expected[storage.id] = mockMergeState(
                    picks = setOf(strings[r][0].id),
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                expected[storage.id] = mockMergeState(
                    picks = setOf(durations[r][0].id),
                )
            }
            testSuite.assertEquals(
                expected = expected,
                actual = transmitter.getMergeStates(syncStates = receiver.getSyncStates()),
                assert = testSuite::assertEquals,
            )
        }
    }
}
