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
        val storages = testSuite.storages(types = setOf(String::class.java))
        val storage = storages[String::class.java] ?: error("No storage!")
        testSuite.assertEquals(storage, emptyList())
        assertEquals(null, storage[UUID(42, 0)])
        val p0 = testSuite.add(storage)
        assertEquals(null, storage[UUID(42, 0)])
        val actual = storage[p0.id] ?: error("No payload!")
        testSuite.assertEquals(expected = p0, actual = actual)
    }

    @Test
    fun addTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val storages = testSuite.storages(types = setOf(String::class.java))
        val storage = storages[String::class.java] ?: error("No storage!")
        testSuite.assertEquals(storage, emptyList())
        assertEquals(null, storage[UUID(42, 0)])
        val p0 = testSuite.add(storage)
        testSuite.assertEquals(expected = p0, actual = storage.payloads.single())
        assertEquals(null, storage[UUID(42, 0)])
        testSuite.assertEquals(expected = p0, actual = storage[p0.id] ?: error("No payload!"))
        val p1 = testSuite.add(storage)
        testSuite.assertEquals(storage, listOf(p0, p1))
    }

    @Test
    fun updateTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val storages = testSuite.storages(types = setOf(String::class.java))
        val storage = storages[String::class.java] ?: error("No storage!")
        testSuite.assertEquals(storage, emptyList())
        assertEquals(null, storage[UUID(42, 0)])
        val p0 = testSuite.add(storage)
        assertEquals(null, storage.update(UUID(42, 0), "foobarbaz"))
        val expected = testSuite.update(storage = storage, id = p0.id)
        testSuite.assertEquals(storage, listOf(expected))
    }

    @Test
    fun deleteTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val storages = testSuite.storages(types = setOf(String::class.java))
        val storage = storages[String::class.java] ?: error("No storage!")
        testSuite.assertEquals(storage, emptyList())
        assertEquals(null, storage[UUID(42, 0)])
        val p0 = testSuite.add(storage = storage)
        testSuite.assertEquals(storage, listOf(p0))
        assertEquals(false, storage.delete(id = UUID(42, 0)))
        testSuite.delete(storage = storage, id = p0.id)
        assertEquals(null, storage[p0.id])
        testSuite.assertEquals(storage, emptyList())
    }
}
