package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.streamers.MutableFileStreamer
import sp.kx.streamers.MutableStreamer
import sp.kx.times.Times
import java.io.File
import java.util.UUID

@Deprecated(message = "mockMutableStorage")
fun <T : Any> mockSyncStorage(
    id: UUID = UUID(0, 0),
    streamer: MutableStreamer = MutableFileStreamer(src = File.createTempFile("foo", "bar")),
    transformer: Transformer<T>,
    hashes: Hashes = Hashes.MD5,
    times: Times = MockTimes(),
    ids: Ids = MockIds(),
): SyncStorage<T> {
    return RealSyncStorage(
        id = id,
        streamer = streamer,
        transformer = transformer,
        hashes = hashes,
        times = times,
        ids = ids,
    )
}
