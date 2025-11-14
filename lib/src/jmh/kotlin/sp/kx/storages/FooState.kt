package sp.kx.storages

import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.ids.RealIds
import sp.kx.times.RealTimes
import sp.kx.times.Times
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@State(Scope.Thread)
internal open class FooState(
    files: File = Benchmarks.files,
    counts: Set<Int> = setOf(128, 512, 1024, 8192),
) {
    val holders: Map<Int, StoragesHolder<Foo>>

    init {
        val index = indices.incrementAndGet()
        holders = counts.associateWith { count ->
            val storages = RealSyncStorages.Builder()
                .add(UUID(0, 0), Foo::class.java, FooTransformer)
                .build(
                    files = files.resolve("storages-$index-$count").also { check(it.mkdir()) },
                    hashes = hashes,
                    times = times,
                    ids = ids,
                )
            val storage = storages[Foo::class.java] ?: error(" No storage!")
            val values = (0 until count).map(FooTransformer::value)
            storage.addAll(values = values)
            StoragesHolder(storages, Foo::class.java)
        }
    }

    companion object {
        private val hashes: Hashes = Hashes.MD5
        private val times: Times = RealTimes()
        private val ids: Ids = RealIds()
        private val indices = AtomicInteger(-1)
    }
}
