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
        issuers.forEach { storages ->
            testSuite.add<String>(storages, count = 2)
            testSuite.add<Duration>(storages, count = 2)
        }
        issuers[0].also { receiver ->
            val transmitter = issuers[1]
            val expected = mutableMapOf<UUID, MergeState>()
            String::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                expected[storage.id] = mockMergeState(
                    picks = testSuite.storage(receiver, type).payloads.map { it.id }.toSet(),
                    gives = storage.payloads.map {
                        Transformers.map(it, Transformers.Strings)
                    },
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(transmitter, type)
                expected[storage.id] = mockMergeState(
                    picks = testSuite.storage(receiver, type).payloads.map { it.id }.toSet(),
                    gives = storage.payloads.map {
                        Transformers.map(it, Transformers.Durations)
                    },
                )
            }
            testSuite.assertEquals(
                expected = expected,
                actual = transmitter.getMergeStates(syncStates = receiver.getSyncStates()),
                assert = { _, expected, actual -> testSuite.assertEquals(expected = expected, actual = actual) }
            )
        }
    }
}
