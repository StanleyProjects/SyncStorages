package sp.kx.storages

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@Fork(value = 1, warmups = 0)
@Warmup(iterations = 0)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(Threads.MAX)
internal open class GetBenchmark : Benchmarks() {
    @Param(value = ["128", "512", "1024", "8192"])
    var count: Int = 0

    @Benchmark
    fun getFirstBenchmark(hole: Blackhole, state: FooState) {
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

    @Benchmark
    fun getMidBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        val expected = holder.mid
        val actual = holder.storage[expected.id]
        checkNotNull(actual)
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.value == actual.value)
        hole.consume(actual)
    }

    @Benchmark
    fun getLastBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        val expected = holder.last
        val actual = holder.storage[expected.id]
        checkNotNull(actual)
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.value == actual.value)
        hole.consume(actual)
    }

    @Benchmark
    fun getRandomBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        val expected = holder.random
        val actual = holder.storage[expected.id]
        checkNotNull(actual)
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.value == actual.value)
        hole.consume(actual)
    }

    @Benchmark
    fun getNoneBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        val actual = holder.storage[holder.none]
        check(actual == null)
        hole.consume(holder.none)
    }
}
