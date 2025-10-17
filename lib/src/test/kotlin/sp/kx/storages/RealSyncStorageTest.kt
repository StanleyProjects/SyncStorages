package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import sp.kx.hashes.Hashes
import sp.kx.streamers.MutableFileStreamer
import java.io.File
import java.util.UUID

internal class RealSyncStorageTest {
    @Test
    fun getTest() {
        val storage = RealSyncStorage(
            id = UUID(0, 0),
            streamer = MutableFileStreamer(src = File.createTempFile("foo", "bar")),
            transformer = StringTransformer,
            hashes = Hashes.MD5,
        )
        assertNull(storage[UUID(0, 0)])
    }
}
