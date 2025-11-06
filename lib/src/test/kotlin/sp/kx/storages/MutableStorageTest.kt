package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID

internal class MutableStorageTest {
    @Test
    fun getTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(UUID(0, 0), String::class.java, Transformers.Strings)
        val storages = testSuite.storages(builder = builder)
        val storage = testSuite.storage(storages, String::class.java)
        testSuite.assertEquals(storage, emptyList())
        assertEquals(null, storage[UUID(42, 0)])
        val p0 = testSuite.add(storage)
        assertEquals(null, storage[UUID(42, 0)])
        testSuite.assertEquals(expected = p0, actual = testSuite.payload(storage, p0.id))
        testSuite.assertEquals(storage, listOf(p0))
    }

    @Test
    fun addTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(UUID(0, 0), String::class.java, Transformers.Strings)
        val storages = testSuite.storages(builder = builder)
        val storage = testSuite.storage(storages, String::class.java)
        testSuite.assertEquals(storage, emptyList())
        val p0 = testSuite.add(storage)
        testSuite.assertEquals(expected = p0, actual = testSuite.payload(storage, p0.id))
        testSuite.assertEquals(storage, listOf(p0))
        assertEquals(null, storage[UUID(42, 0)])
        val p1 = testSuite.add(storage)
        testSuite.assertEquals(expected = p0, actual = testSuite.payload(storage, p0.id))
        testSuite.assertEquals(expected = p1, actual = testSuite.payload(storage, p1.id))
        testSuite.assertEquals(storage, listOf(p0, p1))
    }

    @Test
    fun updateTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(UUID(0, 0), String::class.java, Transformers.Strings)
        val storages = testSuite.storages(builder = builder)
        val storage = testSuite.storage(storages, String::class.java)
        testSuite.assertEquals(storage, emptyList())
        val p0 = testSuite.add(storage)
        assertEquals(null, storage.update(UUID(42, 0), "foobarbaz"))
        val expected = testSuite.update(storage = storage, id = p0.id)
        assertEquals(p0.id, expected.id)
        assertEquals(p0.created, expected.created)
        check(p0.created < expected.updated)
        check(p0.updated < expected.updated)
        check(p0.value != expected.value)
        testSuite.assertEquals(expected = expected, actual = testSuite.payload(storage, p0.id))
        testSuite.assertEquals(storage, listOf(expected))
    }

    @Test
    fun deleteTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val builder = RealSyncStorages.Builder()
            .add(UUID(0, 0), String::class.java, Transformers.Strings)
        val storages = testSuite.storages(builder = builder)
        val storage = testSuite.storage(storages, String::class.java)
        testSuite.assertEquals(storage, emptyList())
        val p0 = testSuite.add(storage = storage)
        testSuite.assertEquals(expected = p0, actual = testSuite.payload(storage, p0.id))
        testSuite.assertEquals(storage, listOf(p0))
        assertEquals(false, storage.delete(id = UUID(42, 0)))
        testSuite.delete(storage = storage, id = p0.id)
        assertEquals(null, storage[p0.id])
        testSuite.assertEquals(storage, emptyList())
    }
}
