package sp.kx.storages

import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.io.File

internal class SyncStoragesTestSuite {
    val hashes: Hashes
    val times: Times
    val ids: Ids
    val s1: SyncStorages
    val s2: SyncStorages

    constructor(
        dir: File,
        hashes: Hashes = Hashes.MD5,
        times: Times = MockTimes(),
        ids: Ids = MockIds(),
    ) {
        this.hashes = hashes
        this.times = times
        this.ids = ids
        s1 = RealSyncStorages.Builder()
            .add(id = ids.random(), type = String::class.java, transformer = Transformers.Strings)
            .add(id = ids.random(), type = Int::class.java, transformer = Transformers.Ints)
            .build(
                dir = dir,
                hashes = hashes,
                times = times,
                ids = ids,
            )
        val s11 = s1[String::class.java] ?: error("No storage!")
        val s12 = s1[Int::class.java] ?: error("No storage!")
        s2 = RealSyncStorages.Builder()
            .add(id = s11.id, type = String::class.java, transformer = Transformers.Strings)
            .add(id = s12.id, type = Int::class.java, transformer = Transformers.Ints)
            .build(
                dir = dir,
                hashes = hashes,
                times = times,
                ids = ids,
            )
    }

    fun hashOf(value: String): ByteArray {
        return hashes.map(Transformers.Strings.encode(value))
    }
}
