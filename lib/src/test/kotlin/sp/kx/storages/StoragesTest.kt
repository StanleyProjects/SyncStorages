package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class StoragesTest {
    @Test
    fun getTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(Keys.Strings, Transformers.Strings)
        val storages = testSuite.storages(builder = builder)
        assertTrue(storages[Keys.Strings] != null)
        assertTrue(storages[Keys.Durations] == null)
    }
}
