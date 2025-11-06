package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration

internal class SyncStatesTest {
    @Test
    fun getSyncStatesTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val types = setOf(String::class.java, Duration::class.java)
        val issuers = (0 until 2).map { _ ->
            testSuite.storages(types = types)
        }
        issuers.forEach { storages ->
            testSuite.add<String>(storages, count = 2)
            testSuite.add<Duration>(storages, count = 2)
        }
        issuers.forEach { storages ->
            testSuite.assertEquals(
                expected = types.associate { type ->
                    val storage = testSuite.storage(storages, type)
                    storage.id to mockSyncState(
                        valueStates = storage.payloads.associate { payload ->
                            payload.id to mockValueState(
                                updated = payload.updated,
                                hash = testSuite.hashOf(payload = payload, type = type),
                            )
                        },
                    )
                },
                actual = storages.getSyncStates(),
                assert = { _, expected, actual -> testSuite.assertEquals(expected = expected, actual = actual) }
            )
        }
    }
}
