package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.streamers.MutableFileStreamer
import sp.kx.streamers.MutableStreamer
import java.io.File
import java.util.UUID

fun <T : Any> mockSyncStorage(
    id: UUID = UUID(0, 0),
    streamer: MutableStreamer = MutableFileStreamer(src = File.createTempFile("foo", "bar")),
    transformer: Transformer<T>,
    hashes: Hashes = Hashes.MD5,
): SyncStorage<T> {
    return RealSyncStorage(
        id = id,
        streamer = streamer,
        transformer = transformer,
        hashes = hashes,
    )
}
