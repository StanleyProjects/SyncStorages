package sp.kx.storages

import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import java.io.File

@State(Scope.Benchmark)
internal open class Benchmarks(private val files: File = Benchmarks.files) {
    @Setup(Level.Trial)
    fun eachTrial() {
        if (files.exists()) {
            if (!files.isDirectory) TODO()
            files.deleteRecursively()
        }
        check(files.mkdirs())
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        files.deleteRecursively()
    }

    companion object {
        val files = File("/tmp/storages")
    }
}
