package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import sp.kx.hashes.Hashes
import java.util.HexFormat
import java.util.UUID
import java.io.File

internal class MutableStorageTest {
    @Test
    fun getTest(@TempDir dir: File) {
        val storage = mockMutableStorage(transformer = StringTransformer, dir = dir)
        assertNull(storage[UUID(0, 0)])
    }

    @Test
    fun addTest(@TempDir dir: File) {
        val storage = mockMutableStorage(
            transformer = StringTransformer,
            dir = dir,
            hashes = Hashes.MD5,
        )
        val value = "foo bar baz"
        val payload = storage.add(value = value)
        assertEquals(value, payload.value)
        val expected = HexFormat.of().parseHex("ab07acbb1e496801937adfa772424bf7")
        assertTrue(expected.contentEquals(payload.valueState.hash))
        //
        val actual = storage[payload.valueInfo.id]
        checkNotNull(actual)
        assertEquals(payload.value, actual.value)
        assertEquals(payload.valueInfo, actual.valueInfo)
        assertEquals(payload.valueState.updated, actual.valueState.updated)
        assertTrue(payload.valueState.hash.contentEquals(actual.valueState.hash))
    }

    @Test
    fun deleteTest(@TempDir dir: File) {
        val storage = mockMutableStorage(
            transformer = StringTransformer,
            dir = dir,
            hashes = Hashes.MD5,
        )
        val value = "foo bar baz"
        val payload = storage.add(value = value)
        assertEquals(value, payload.value)
        val expected = HexFormat.of().parseHex("ab07acbb1e496801937adfa772424bf7")
        assertTrue(expected.contentEquals(payload.valueState.hash))
        //
        val actual = storage[payload.valueInfo.id]
        checkNotNull(actual)
        assertEquals(payload.value, actual.value)
        assertEquals(payload.valueInfo, actual.valueInfo)
        assertEquals(payload.valueState.updated, actual.valueState.updated)
        assertTrue(payload.valueState.hash.contentEquals(actual.valueState.hash))
        //
        assertTrue(storage.delete(payload.valueInfo.id))
        assertNull(storage[payload.valueInfo.id])
    }

    @Test
    fun updateTest(@TempDir dir: File) {
        val storage = mockMutableStorage(
            transformer = StringTransformer,
            dir = dir,
            hashes = Hashes.MD5,
        )
        val payload = storage.add(value = "foo bar baz")
        //
        val value = "qux"
        val valueState = storage.update(id = payload.valueInfo.id, value = value)
        checkNotNull(valueState)
        val expected = HexFormat.of().parseHex("d85b1213473c2fd7c2045020a6b9c62b")
        assertTrue(expected.contentEquals(valueState.hash))
        val actual = storage[payload.valueInfo.id]
        checkNotNull(actual)
        assertEquals(value, actual.value)
        assertEquals(payload.valueInfo, actual.valueInfo)
        assertEquals(valueState.updated, actual.valueState.updated)
        assertTrue(valueState.hash.contentEquals(actual.valueState.hash))
    }
}
