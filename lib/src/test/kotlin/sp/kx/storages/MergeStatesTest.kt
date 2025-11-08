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
}
