package sp.kx.storages

import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.ids.RealIds
import sp.kx.times.RealTimes
import sp.kx.times.Times
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

internal open class FooState(parent: File, counts: Set<Int>) {
    val holders = counts.associateWith { count ->
        val storages = RealSyncStorages.Builder()
            .add(UUID(0, 0), Foo::class.java, FooTransformer)
            .build(
                dir = parent.resolve("storages-${indices.incrementAndGet()}-$count").also { check(it.mkdir()) },
                hashes = hashes,
                times = times,
                ids = ids,
            )
        val storage = storages[Foo::class.java] ?: error(" No storage!")
        for (index in 0 until count) {
            storage.add(FooTransformer.value(index = index))
        }
        StoragesHolder(storages, Foo::class.java)
    }

    companion object {
        private val hashes: Hashes = Hashes.MD5
        private val times: Times = RealTimes()
        private val ids: Ids = RealIds()
        private val indices = AtomicInteger(-1)
    }
}
