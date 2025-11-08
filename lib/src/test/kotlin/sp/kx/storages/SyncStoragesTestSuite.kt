package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import sp.kx.bytes.Transformer
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
    private val indices = AtomicInteger(-1)

    fun storages(builder: RealSyncStorages.Builder): SyncStorages {
        return builder.build(
            dir = dir.resolve("storages_${indices.incrementAndGet()}").also { check(it.mkdir()) },
            hashes = hashes,
            times = times,
            ids = ids,
        )
    }

    fun <T : Comparable<T>> storage(storages: MutableStorages, type: Class<T>): MutableStorage<T> {
        return storages[type] ?: error("No storage $type!")
    }

    inline fun <reified T : Comparable<T>> payload(storage: Storage<T>, id: UUID): Payload<T> {
        return storage.get(id = id) ?: error("No payload(${T::class.java}) $id!")
    }

    inline fun <reified T : Comparable<T>> assertEquals(expected: Payload<T>, actual: Payload<T>) {
        assertEquals(expected.id, actual.id, "Payload<${T::class.java.simpleName}>:id")
        assertEquals(expected.created, actual.created, "Payload<${T::class.java.simpleName}>:created")
        assertEquals(expected.updated, actual.updated, "Payload<${T::class.java.simpleName}>:updated")
        assertEquals(expected.value, actual.value, "Payload<${T::class.java.simpleName}>:value")
    }

    fun assertEquals(expected: SyncState, actual: SyncState) {
        assertEquals(expected = expected.deleted, actual = actual.deleted)
        assertEquals(
            expected = expected.valueStates,
            actual = actual.valueStates,
            assert = ::assertEquals,
        )
    }

    fun assertEquals(expected: MergeState, actual: MergeState) {
        assertEquals(expected = expected.deleted, actual = actual.deleted)
        assertEquals(expected = expected.picks, actual = actual.picks)
        assertEquals(
            expected = expected.gives,
            actual = actual.gives,
            comparator = Comparators.payloads,
            assert = { i, e, a ->
                assertEquals(e.id, a.id, "MergeState:gives:$i:id")
                assertEquals(e.created, a.created, "MergeState:gives:$i:created")
                assertEquals(e.updated, a.updated, "MergeState:gives:$i:updated")
                assertTrue(e.value.contentEquals(a.value), "MergeState:gives:$i:value")
            }
        )
    }

    fun assertEquals(expected: CommitState, actual: CommitState) {
        assertEquals(expected = expected.deleted, actual = actual.deleted)
        assertEquals(
            expected = expected.gives,
            actual = actual.gives,
            comparator = Comparators.payloads,
            assert = { i, e, a ->
                assertEquals(e.id, a.id, "CommitState:gives:$i:id")
                assertEquals(e.created, a.created, "CommitState:gives:$i:created")
                assertEquals(e.updated, a.updated, "CommitState:gives:$i:updated")
                assertTrue(e.value.contentEquals(a.value), "CommitState:gives:$i:value")
            },
        )
        assertTrue(expected.hash.contentEquals(actual.hash), "CommitState:hash")
    }

    inline fun <reified T : Any> assertEquals(
        expected: Collection<T>,
        actual: Collection<T>,
        comparator: Comparator<in T>,
        assert: (i: Int, e: T, a: T) -> Unit,
    ) {
        assertEquals(expected.size, actual.size, T::class.java.simpleName)
        val sorted = actual.sortedWith(comparator)
        expected.sortedWith(comparator).forEachIndexed { i, e ->
            assert(i, e, sorted[i])
        }
    }

    inline fun <reified T : Comparable<T>> assertEquals(expected: Collection<T>, actual: Collection<T>) {
        assertEquals(expected.size, actual.size, T::class.java.simpleName)
        val sorted = actual.sorted()
        expected.sorted().forEachIndexed { index, e ->
            assertEquals(e, sorted[index], "${T::class.java.simpleName}:$index")
        }
    }

    inline fun <reified T : Comparable<T>> assertEquals(
        storage: Storage<T>,
        expected: Collection<Payload<T>>,
    ) {
        assertEquals(
            expected = expected,
            actual = storage.payloads,
            comparator = Comparators.payloads,
            assert = { _, e, a -> assertEquals(expected = e, actual = a) },
        )
    }

    inline fun <reified K : Comparable<K>, reified V : Any> assertEquals(
        expected: Map<K, V>,
        actual: Map<K, V>,
        assert: (expected: V, actual: V) -> Unit,
    ) {
        assertEquals(expected.keys.size, actual.keys.size, "Map<${K::class.java.simpleName}, ${V::class.java.simpleName}>")
        val sorted = actual.entries.sortedBy { (key, _) -> key }
        expected.entries.sortedBy { (key, _) -> key }.forEachIndexed { index, (key, value) ->
            val entry = sorted[index]
            assertEquals(key, entry.key)
            assert(value, entry.value)
        }
    }

    inline fun <reified T : Comparable<T>> add(storage: MutableStorage<T>, value: T): Payload<T> {
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

    inline fun <reified T : Comparable<T>> add(storage: MutableStorage<T>): Payload<T> {
        return add(storage = storage, value = Transformers.value(T::class.java, indices.incrementAndGet()))
    }

    inline fun <reified T : Comparable<T>> add(storages: MutableStorages, count: Int): List<Payload<T>> {
        check(count > 0)
        val storage = storage(storages, T::class.java)
        return (0 until count).map { add(storage) }
    }

    inline fun <reified T : Comparable<T>> update(storage: MutableStorage<T>, id: UUID, value: T): Payload<T> {
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
        check(payload.created < updated)
        check(payload.updated < updated)
        check(payload.value != value)
        assertEquals(
            expected = expected,
            actual = storage[id] ?: error("No payload $id!"),
        )
        return expected
    }

    inline fun <reified T : Comparable<T>> update(storage: MutableStorage<T>, id: UUID): Payload<T> {
        return update(storage = storage, id = id, value = Transformers.value(T::class.java, indices.incrementAndGet()))
    }

    fun <T : Comparable<T>> delete(storage: MutableStorage<T>, id: UUID) {
        val before = storage.payloads
        val payload = storage[id] ?: error("No payload $id!")
        check(storage.delete(id = id))
        val after = storage.payloads
        check(before.size == after.size + 1)
        check(before.count { it.id == payload.id } == 1)
        check(after.none { it.id == payload.id })
        check(storage[payload.id] == null)
    }

    inline fun <reified T : Comparable<T>> hashOf(payload: Payload<out T>): ByteArray {
        val transformer = Transformers.get(T::class.java)
        return hashes.map(transformer.encode(payload.value))
    }

    inline fun <reified T : Comparable<T>> hashOf(payloads: List<Payload<out T>>): ByteArray {
        return SyncStorageAlgorithms.hashOf(payloads = payloads.map(Transformers::map), hashes = hashes)
    }
}
