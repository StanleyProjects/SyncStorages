package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

internal class SyncStoragesTestSuite(
    private val dir: File,
    private val hashes: Hashes = Hashes.MD5,
    private val times: Times = MockTimes(),
    private val ids: Ids = MockIds(),
) {
    private val indices = AtomicInteger(-1)

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

    fun <T : Any> assertEquals(
        expected: Collection<T>,
        actual: Collection<T>,
        comparator: Comparator<in T>,
        assert: (index: Int, expected: T, actual: T) -> Unit,
    ) {
        assertEquals(expected.size, actual.size)
        val sorted = actual.sortedWith(comparator)
        expected.sortedWith(comparator).forEachIndexed { index, e ->
            assert(index, e, sorted[index])
        }
    }

    fun <T : Comparable<T>> assertEquals(
        storage: Storage<T>,
        expected: Collection<Payload<T>>,
    ) {
        assertEquals(
            expected = expected,
            actual = storage.payloads,
            comparator = Comparators.payloads,
            assert = { _, expected, actual -> assertEquals(expected = expected, actual = actual) },
        )
    }

    fun <T : Comparable<T>> add(storage: MutableStorage<T>, value: T): Payload<T> {
        val before = storage.payloads
        val payload = storage.add(value = value)
        val after = storage.payloads
        check(before.size == after.size - 1)
        check(before.none { it.id == payload.id })
        assertEquals(expected = payload, actual = after.single { it.id == payload.id })
        assertEquals(expected = payload, actual = storage[payload.id]!!)
        check(payload.value == value)
        check(payload.created == payload.updated)
        return payload
    }

    fun add(storage: MutableStorage<String>): Payload<String> {
        return add(storage = storage, value = "value:${indices.incrementAndGet()}")
    }

    fun <T : Comparable<T>> update(storage: MutableStorage<T>, id: UUID, value: T): Duration {
        val before = storage.payloads
        val payload = storage[id] ?: error("No payload $id!")
        val updated = storage.update(id = id, value = value) ?: error("Update error!")
        check(before.size == storage.payloads.size)
        val expected = Payload(
            id = id,
            created = payload.created,
            updated = updated,
            value = value,
        )
        check(payload.updated != updated)
        check(payload.value != value)
        assertEquals(
            expected = expected,
            actual = storage[id] ?: error("No payload $id!"),
        )
        return updated
    }

    fun update(storage: MutableStorage<String>, id: UUID): Duration {
        return update(storage = storage, id = id, value = "value:${indices.incrementAndGet()}")
    }
}
