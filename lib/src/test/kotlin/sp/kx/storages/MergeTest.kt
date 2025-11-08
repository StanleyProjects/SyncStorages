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
                    gives = emptyList(),
                    hash = testSuite.hashOf(payloads = payloads),
                )
            }
            Duration::class.java.also { type ->
                val storage = testSuite.storage(receiver, type)
                val payloads = testSuite.storage(transmitter, type).payloads
                expected[storage.id] = mockCommitState(
                    gives = emptyList(),
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
}
