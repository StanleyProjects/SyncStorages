package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

internal class SyncStoragesTestSuite(
    private val dir: File,
    private val hashes: Hashes = Hashes.MD5,
    private val times: Times = MockTimes(),
    private val ids: Ids = MockIds(),
) {
    private val indices = AtomicInteger()

    fun storages(types: Set<Class<out Comparable<*>>>): SyncStorages {
        val builder = RealSyncStorages.Builder()
        types.forEachIndexed { index, type ->
            val transformer = when (type) {
                String::class.java -> Transformers.Strings
                else -> error("No transformer by $type!")
            }
            builder.add(UUID(index.toLong(), 0), type, transformer)
        }
        return builder.build(
            dir = dir.resolve("storages_${indices.incrementAndGet()}").also { check(it.mkdir()) },
            hashes = hashes,
            times = times,
            ids = ids,
        )
    }

    fun <T : Comparable<T>> assertEquals(expected: Payload<T>, actual: Payload<T>) {
        assertEquals(expected.id, actual.id)
        assertEquals(expected.created, actual.created)
        assertEquals(expected.updated, actual.updated)
        assertEquals(expected.value, actual.value)
    }
}
