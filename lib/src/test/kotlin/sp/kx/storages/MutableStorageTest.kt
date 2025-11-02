package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class MutableStorageTest {
    @Test
    fun getTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val s11 = testSuite.storage<String>(1)
        assertNull(s11[UUID(0, 0)])
        assertTrue(s11.payloads.isEmpty())
        val payload = s11.add(value = "v1")
        assertEquals(expected = payload, actual = s11.payloads.single())
        val actual = s11[payload.id]
        checkNotNull(actual)
        assertEquals(expected = payload, actual = actual)
    }

    @Test
    fun addTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val value = "v1"
        val p111 = testSuite.add(1, value = "v1")
        assertEquals(
            expected = Payload(
                id = UUID(0, 0),
                created = 0.milliseconds,
                value = value,
                updated = 0.milliseconds,
            ),
            actual = p111,
        )
    }

    @Test
    fun deleteTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val s11 = testSuite.storage<String>(1)
        val value = "v1"
        val p111 = testSuite.add(1, value = value)
        assertEquals(value, p111.value)
        //
        val actual = s11[p111.id]
        checkNotNull(actual)
        assertEquals(expected = p111, actual = actual)
        //
        assertTrue(s11.delete(p111.id))
        assertNull(s11[p111.id])
    }

    @Test
    fun updateTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val s11 = testSuite.storage<String>(1)
        val p111 = testSuite.add(1, value = "v1")
        //
        val updated = testSuite.update(1, id = p111.id, value = "v2")
        checkNotNull(updated)
        val actual = s11[p111.id]
        checkNotNull(actual)
        assertEquals("v2", actual.value)
        assertEquals(p111.id, actual.id)
        assertEquals(p111.created, actual.created)
        assertEquals(updated, actual.updated)
    }
}
