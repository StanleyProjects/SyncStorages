package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration

internal class MergeStatesTest {
    @Test
    fun getMergeStatesTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val types = setOf(String::class.java, Duration::class.java)
        val issuers = (0 until 2).map { _ ->
            testSuite.storages(types = types)
        }
        issuers.forEach { storages ->
            testSuite.add<String>(storages, count = 2)
            testSuite.add<Duration>(storages, count = 2)
        }
        testSuite.assertEquals(
            expected = types.associate { type ->
                val storage = testSuite.storage(issuers[0], type)
                storage.id to mockMergeState(
                    picks = testSuite.storage(issuers[1], type).payloads.map { it.id }.toSet(),
                    gives = storage.payloads.map { payload ->
                        Transformers.map(payload = payload, type = type)
                    },
                )
            },
            actual = issuers[0].getMergeStates(syncStates = issuers[1].getSyncStates()),
            assert = { _, expected, actual -> testSuite.assertEquals(expected = expected, actual = actual) }
        )
        testSuite.assertEquals(
            expected = types.associate { type ->
                val storage = testSuite.storage(issuers[1], type)
                storage.id to mockMergeState(
                    picks = testSuite.storage(issuers[0], type).payloads.map { it.id }.toSet(),
                    gives = storage.payloads.map { payload ->
                        Transformers.map(payload = payload, type = type)
                    },
                )
            },
            actual = issuers[1].getMergeStates(syncStates = issuers[0].getSyncStates()),
            assert = { _, expected, actual -> testSuite.assertEquals(expected = expected, actual = actual) }
        )
    }
}
