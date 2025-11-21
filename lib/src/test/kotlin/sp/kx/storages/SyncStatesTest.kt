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
        issuers.forEachIndexed { index, storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, key = Keys.Strings),
                expected = strings[index],
            )
        }
        issuers.forEachIndexed { index, storages ->
            testSuite.assertEquals(
                storage = testSuite.storage(storages, key = Keys.Durations),
                expected = durations[index],
            )
        }
        issuers.forEachIndexed { index, storages ->
            val expected = mutableMapOf<UUID, SyncState>()
            Keys.Strings.also { key ->
                val storage = testSuite.storage(storages, key = key)
                expected[storage.id] = mockSyncState(
                    valueStates = strings[index].associate { payload ->
                        payload.id to mockValueState(
                            updated = payload.updated,
                            hash = testSuite.hashOf(payload),
                        )
                    },
                )
            }
            Keys.Durations.also { key ->
                val storage = testSuite.storage(storages, key = key)
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
                storage = testSuite.storage(storages, key = Keys.Durations),
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
        issuers.forEachIndexed { index, storages ->
            val expected = mutableMapOf<UUID, SyncState>()
            Keys.Strings.also { key ->
                val storage = testSuite.storage(storages, key = key)
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
            Keys.Durations.also { key ->
                val storage = testSuite.storage(storages, key = key)
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
