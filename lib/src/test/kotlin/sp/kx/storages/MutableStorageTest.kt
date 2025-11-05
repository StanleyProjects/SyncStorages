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
        assertEquals(0, storage.payloads.size)
        assertEquals(null, storage[UUID(42, 0)])
        val value = "foobarbaz"
        val payload = storage.add(value = value)
        assertEquals(payload.value, value)
        testSuite.assertEquals(expected = payload, actual = storage.payloads.single())
        assertEquals(null, storage[UUID(42, 0)])
        val actual = storage[payload.id] ?: error("No payload!")
        testSuite.assertEquals(expected = payload, actual = actual)
    }
}
