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
        val s11 = testSuite.s1[String::class.java] ?: error("No storage!")
        assertNull(s11[UUID(0, 0)])
        val value = "v1"
        val payload = s11.add(value = value)
        val actual = s11[payload.valueInfo.id]
        checkNotNull(actual)
        assertEquals(expected = payload, actual = actual)
    }

    @Test
    fun addTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val s11 = testSuite.s1[String::class.java] ?: error("No storage!")
        val value = "v1"
        val payload = s11.add(value = value)
        assertEquals(
            expected = Payload(
                value = value,
                valueInfo = ValueInfo(
                    id = UUID(0, 2),
                    created = 0.milliseconds,
                ),
                valueState = ValueState(
                    updated = 0.milliseconds,
                    hash = testSuite.hashOf(value = value),
                ),
            ),
            actual = payload,
        )
    }

    @Test
    fun deleteTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val s11 = testSuite.s1[String::class.java] ?: error("No storage!")
        val value = "v1"
        val payload = s11.add(value = value)
        assertEquals(value, payload.value)
        assertTrue(testSuite.hashOf(value = value).contentEquals(payload.valueState.hash))
        //
        val actual = s11[payload.valueInfo.id]
        checkNotNull(actual)
        assertEquals(payload.value, actual.value)
        assertEquals(payload.valueInfo, actual.valueInfo)
        assertEquals(payload.valueState.updated, actual.valueState.updated)
        assertTrue(payload.valueState.hash.contentEquals(actual.valueState.hash))
        //
        assertTrue(s11.delete(payload.valueInfo.id))
        assertNull(s11[payload.valueInfo.id])
    }

    @Test
    fun updateTest(@TempDir dir: File) {
        val testSuite = SyncStoragesTestSuite(dir = dir)
        val s11 = testSuite.s1[String::class.java] ?: error("No storage!")
        val payload = s11.add(value = "v1")
        //
        val valueState = s11.update(id = payload.valueInfo.id, value = "v2")
        checkNotNull(valueState)
        assertTrue(testSuite.hashOf(value = "v2").contentEquals(valueState.hash))
        val actual = s11[payload.valueInfo.id]
        checkNotNull(actual)
        assertEquals("v2", actual.value)
        assertEquals(payload.valueInfo, actual.valueInfo)
        assertEquals(valueState, actual.valueState)
    }
}
