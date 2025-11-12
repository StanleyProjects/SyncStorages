package sp.kx.storages

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.infra.Blackhole
import java.io.File
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
internal open class GetState() : FooState(parent = File("/tmp/storages"), counts = setOf(128))

@State(Scope.Benchmark)
internal open class GetBenchmark {
    @Param(value = ["128"])
    var count: Int = 0
    private val parent = File("/tmp/storages")

    @Setup(Level.Trial)
    fun eachTrial() {
        if (parent.exists()) {
            if (!parent.isDirectory) TODO()
            parent.deleteRecursively()
        }
        check(parent.mkdirs())
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        parent.deleteRecursively()
    }

    @Benchmark
    @Fork(value = 1, warmups = 0)
    @Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.NANOSECONDS)
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    fun getFirstBenchmark(hole: Blackhole, state: GetState) {
        val holder = state.holders[count] ?: error("No holder!")
        val expected = holder.first
        val actual = holder.storage[expected.id]
        checkNotNull(actual)
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.value == actual.value)
        hole.consume(actual)
    }
}
