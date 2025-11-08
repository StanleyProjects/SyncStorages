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
        issuers.forEach { storages ->
            testSuite.add<String>(storages, count = 2)
            testSuite.add<Duration>(storages, count = 2)
        }
        issuers.forEach { storages ->
            val expected = mutableMapOf<UUID, SyncState>()
            testSuite.storage(storages, String::class.java).also { storage ->
                expected[storage.id] = mockSyncState(
                    valueStates = storage.payloads.associate { payload ->
                        payload.id to mockValueState(
                            updated = payload.updated,
                            hash = testSuite.hashOf(payload),
                        )
                    },
                )
            }
            testSuite.storage(storages, Duration::class.java).also { storage ->
                expected[storage.id] = mockSyncState(
                    valueStates = storage.payloads.associate { payload ->
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
