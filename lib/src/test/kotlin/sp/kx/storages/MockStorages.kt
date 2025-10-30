package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.io.File
import java.util.UUID

internal inline fun <reified T : Any> mockMutableStorage(
    id: UUID = UUID(0, 0),
    dir: File,
    transformer: Transformer<T>,
    hashes: Hashes = Hashes.MD5,
    times: Times = MockTimes(),
    ids: Ids = MockIds(),
): MutableStorage<T> {
    val storages: MutableStorages = RealSyncStorages.Builder()
        .add(id = id, type = T::class.java, transformer = transformer)
        .build(
            dir = dir,
            hashes = hashes,
            times = times,
            ids = ids,
        )
    return storages[T::class.java] ?: error("No storage!")
}

internal inline fun <reified T : Any> mockSyncStorages(
    id: UUID = UUID(0, 0),
    dir: File,
    transformer: Transformer<T>,
    hashes: Hashes = Hashes.MD5,
    times: Times = MockTimes(),
    ids: Ids = MockIds(),
): SyncStorages {
    return RealSyncStorages.Builder()
        .add(id = id, type = T::class.java, transformer = transformer)
        .build(
            dir = dir,
            hashes = hashes,
            times = times,
            ids = ids,
        )
}
