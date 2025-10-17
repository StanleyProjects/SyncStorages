package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.hashes.Hashes
import java.util.HexFormat
import java.util.UUID

internal class RealSyncStorageTest {
    @Test
    fun getTest() {
        val storage = mockSyncStorage(transformer = StringTransformer)
        assertNull(storage[UUID(0, 0)])
    }

    @Test
    fun addTest() {
        val storage = mockSyncStorage(
            transformer = StringTransformer,
            hashes = Hashes.MD5,
        )
        val value = "foo bar baz"
        val payload = storage.add(value = value)
        assertEquals(value, payload.value)
        val expected = HexFormat.of().parseHex("ab07acbb1e496801937adfa772424bf7")
        assertTrue(expected.contentEquals(payload.valueState.hash))
    }
}
