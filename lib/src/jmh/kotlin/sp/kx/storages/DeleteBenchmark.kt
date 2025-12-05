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
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
internal open class DeleteBenchmark : Benchmarks() {
    @Param(value = ["128", "512", "1024", "8192"])
    var count: Int = 0

    @Benchmark
    fun deleteFirstBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        hole.consume(holder.storage.delete(id = holder.first.id))
    }

    @Benchmark
    fun deleteMidBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        hole.consume(holder.storage.delete(id = holder.mid.id))
    }

    @Benchmark
    fun deleteLastBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        hole.consume(holder.storage.delete(id = holder.last.id))
    }

    @Benchmark
    fun deleteRandomBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        hole.consume(holder.storage.delete(id = holder.random.id))
    }

    @Benchmark
    fun deleteNoneBenchmark(hole: Blackhole, state: FooState) {
        val holder = state.holders[count] ?: error("No holder!")
        hole.consume(holder.storage.delete(id = holder.none))
    }
}
