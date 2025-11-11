package sp.kx.storages

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole
import sp.kx.hashes.Hashes
import sp.kx.ids.RealIds
import sp.kx.times.RealTimes
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@State(Scope.Benchmark)
internal open class MutableStorageBenchmark {
    @Param(value = ["1024"])
    var count: Int = 0
    private var storages: Storages? = null
    private var storage: Storage<Foo>? = null
    private var payloads: List<Payload<Foo>>? = null
    private var first: Payload<Foo>? = null
    private var mid: Payload<Foo>? = null
    private var last: Payload<Foo>? = null
    private val dir = File("/tmp/storages")
    private val times = RealTimes()
    private val ids = RealIds()
    private val builder = RealSyncStorages.Builder()
        .add(UUID(0, 0), Foo::class.java, FooTransformer)

    @Setup(Level.Trial)
    fun eachTrial() {
        if (dir.exists()) {
            if (!dir.isDirectory) TODO()
            check(dir.deleteRecursively())
        }
        check(dir.mkdir())
        val storages = builder.build(
            dir = dir,
            hashes = Hashes.MD5,
            times = times,
            ids = ids,
        )
        val storage = storages[Foo::class.java] ?: error("No storage!")
        for (i in 0 until count) {
            val value = Foo(
                number = i,
                text = "text:$i",
                time = i.milliseconds,
            )
            storage.add(value = value)
        }
        this.storages = storages
        this.storage = storage
        val payloads = storage.payloads
        first = payloads[0]
        mid = payloads[payloads.size / 2]
        last = payloads.lastOrNull()
    }

    @Benchmark
    fun getFirstBenchmark(hole: Blackhole) {
        val storage = storage ?: error("No storage!")
        val expected = first ?: error("No payload!")
        val actual = storage[expected.id]
        checkNotNull(actual)
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.value == actual.value)
        hole.consume(actual)
    }

    @Benchmark
    fun getMidBenchmark(hole: Blackhole) {
        val storage = storage ?: error("No storage!")
        val expected = mid ?: error("No payload!")
        val actual = storage[expected.id]
        checkNotNull(actual)
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.value == actual.value)
        hole.consume(actual)
    }

    @Benchmark
    fun getLastBenchmark(hole: Blackhole) {
        val storage = storage ?: error("No storage!")
        val expected = last ?: error("No payload!")
        val actual = storage[expected.id]
        checkNotNull(actual)
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.value == actual.value)
        hole.consume(actual)
    }
}
