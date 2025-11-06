package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration

internal class SyncStatesTest {
    @Test
    fun fooTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val issuers = (0 until 2).map { _ ->
            testSuite.storages(types = setOf(String::class.java, Duration::class.java))
        }
        issuers.forEach { storages ->
            testSuite.add<String>(storages, count = 2)
            testSuite.add<Duration>(storages, count = 2)
        }
        TODO("SyncStatesTest:fooTest($dir)")
    }
}
