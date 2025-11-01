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
        val s11 = testSuite.require<String>(1)
        assertNull(s11[UUID(0, 0)])
        val payload = s11.add(value = "v1")
        val actual = s11[payload.valueInfo.id]
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
                value = value,
                valueInfo = ValueInfo(
                    id = UUID(0, 0),
                    created = 0.milliseconds,
                ),
                valueState = ValueState(
                    updated = 0.milliseconds,
                    hash = testSuite.hashOf(value = value),
                ),
            ),
            actual = p111,
        )
    }

    @Test
    fun deleteTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val s11 = testSuite.require<String>(1)
        val value = "v1"
        val p111 = testSuite.add(1, value = value)
        assertEquals(value, p111.value)
        assertTrue(testSuite.hashOf(value = value).contentEquals(p111.valueState.hash))
        //
        val actual = s11[p111.valueInfo.id]
        checkNotNull(actual)
        assertEquals(expected = p111, actual = actual)
        //
        assertTrue(s11.delete(p111.valueInfo.id))
        assertNull(s11[p111.valueInfo.id])
    }

    @Test
    fun updateTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val s11 = testSuite.require<String>(1)
        val p111 = testSuite.add(1, value = "v1")
        //
        val valueState = testSuite.update(1, id = p111.valueInfo.id, value = "v2")
        checkNotNull(valueState)
        assertTrue(testSuite.hashOf(value = "v2").contentEquals(valueState.hash))
        val actual = s11[p111.valueInfo.id]
        checkNotNull(actual)
        assertEquals("v2", actual.value)
        assertEquals(p111.valueInfo, actual.valueInfo)
        assertEquals(valueState, actual.valueState)
    }
}
