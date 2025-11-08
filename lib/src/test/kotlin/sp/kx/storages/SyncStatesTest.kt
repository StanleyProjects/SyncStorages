package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import kotlin.time.Duration

internal class SyncStatesTest {
    @Test
    fun getSyncStatesTest(@TempDir dir: File) {
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
        issuers.forEachIndexed { index, storages ->
            val expected = mutableMapOf<UUID, SyncState>()
            String::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                expected[storage.id] = mockSyncState(
                    valueStates = strings[index].associate { payload ->
                        payload.id to mockValueState(
                            updated = payload.updated,
                            hash = testSuite.hashOf(payload),
                        )
                    },
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                expected[storage.id] = mockSyncState(
                    valueStates = durations[index].associate { payload ->
                        payload.id to mockValueState(
                            updated = payload.updated,
                            hash = testSuite.hashOf(payload),
                        )
                    },
                )
            }
            testSuite.assertEquals(
                expected = expected,
                actual = storages.getSyncStates(),
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
            val expected = mutableMapOf<UUID, SyncState>()
            String::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                val deleted = listOf(strings[index][0]).map { it.id }.toSet()
                val payloads = issuers.flatMapIndexed { index, _ -> strings[index] }.filter { !deleted.contains(it.id) }
                expected[storage.id] = mockSyncState(
                    deleted = deleted,
                    valueStates = payloads.associate { payload ->
                        payload.id to mockValueState(
                            updated = payload.updated,
                            hash = testSuite.hashOf(payload),
                        )
                    },
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(storages, type)
                val deleted = listOf(durations[index][0]).map { it.id }.toSet()
                val payloads = issuers.flatMapIndexed { index, _ -> durations[index] }.filter { !deleted.contains(it.id) }
                expected[storage.id] = mockSyncState(
                    deleted = deleted,
                    valueStates = payloads.associate { payload ->
                        payload.id to mockValueState(
                            updated = payload.updated,
                            hash = testSuite.hashOf(payload),
                        )
                    },
                )
            }
            testSuite.assertEquals(
                expected = expected,
                actual = storages.getSyncStates(),
                assert = testSuite::assertEquals,
            )
        }
    }
}
