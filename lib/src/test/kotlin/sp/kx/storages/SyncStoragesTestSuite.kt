package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.io.File
import java.util.UUID

internal class SyncStoragesTestSuite {
    val hashes: Hashes
    val times: Times
    val ids: Ids
    private val stack: List<SyncStorages>
    val s0: SyncStorages get() = stack[0]
    val s1: SyncStorages get() = stack[1]
    val s2: SyncStorages get() = stack[2]

    private inline fun <reified T : Any> RealSyncStorages.Builder.add(
        id: UUID,
        transformer: Transformer<T>,
    ): RealSyncStorages.Builder {
        return add(id = id, type = T::class.java, transformer = transformer)
    }

    constructor(
        dir: File,
        hashes: Hashes = Hashes.MD5,
        times: Times = MockTimes(),
        ids: Ids = MockIds(),
        count: Int = 3,
    ) {
        this.hashes = hashes
        this.times = times
        this.ids = ids
        this.stack = (0 until count).map { index ->
            var mostSigBits = 0L
            RealSyncStorages.Builder()
                .add(UUID(mostSigBits++, 0), Transformers.Strings)
                .add(UUID(mostSigBits++, 0), Transformers.Ints)
                .build(
                    dir = dir.resolve("storages-$index").also { check(it.mkdir()) },
                    hashes = hashes,
                    times = times,
                    ids = ids,
                )
        }
    }

    fun hashOf(value: String): ByteArray {
        return hashes.map(Transformers.Strings.encode(value))
    }

    fun hashOf(value: Int): ByteArray {
        return hashes.map(Transformers.Ints.encode(value))
    }

    fun hashOf(payloads: List<Payload<ByteArray>>): ByteArray {
        return SyncStorageAlgorithms.hashOf(payloads = payloads.sortedWith(Comparators.payloads), hashes = hashes)
    }

    private var strings = 0
    private var ints = 0

    fun merge(index: Int, mergeStates: Map<UUID, MergeState>, expected: Map<UUID, CommitState>) {
        assertEquals(
            expected = expected,
            actual = storages(index = index).merge(mergeStates),
            assert = { _, expected, actual -> expected.assertEquals(actual = actual) },
        )
    }

    fun <T : Comparable<T>> commitState(
        deleted: Set<UUID> = emptySet(),
        gives: List<Payload<T>> = emptyList(),
        expected: List<Payload<T>>,
        transform: (Payload<T>) -> Payload<ByteArray>,
    ): CommitState {
        return CommitState(
            deleted = deleted,
            gives = gives.map(transform),
            hash = hashOf(payloads = expected.map(transform)),
        )
    }

    fun <T : Any> add(index: Int, type: Class<out T>, count: Int): List<Payload<T>> {
        return (0 until count).map { add(index = index, type = type) }
    }

    fun <T : Any> add(index: Int, type: Class<out T>): Payload<T> {
        return when (type) {
            String::class.java -> {
                add(index = index, value = "value:${strings++}")
            }
            Integer::class.java, Int::class.java -> {
                val value = 1_000_000 + ints++
                add(index = index, value = value)
            }
            else -> error("Type $type is not supported!")
        } as Payload<T>
    }

    inline fun <reified T : Any> add(index: Int): Payload<T> {
        return add(index = index, type = T::class.java)
    }

    inline fun <reified T : Comparable<T>> add(index: Int, value: T): Payload<T> {
        val storage = storage<T>(index = index)
        val payload = storage.add(value = value)
        check(payload.value == value)
        check(payload.created == payload.updated)
        return payload
    }

    inline fun <reified T : Comparable<T>> delete(index: Int, id: UUID): Boolean {
        val storage = storage<T>(index = index)
        val before = storage[id]
        val deleted = storage.delete(id = id)
        if (before == null) {
            check(!deleted)
            return false
        }
        check(deleted)
        check(storage.payloads.none { it.id == before.id })
        return true
    }

    inline fun <reified T : Comparable<T>> update(index: Int, id: UUID, value: T): Payload<T>? {
        val storage = storage<T>(index = index)
        val before = storage[id]
        val updated = storage.update(id = id, value = value)
        if (before == null) {
            check(updated == null)
            return null
        }
        check(before.value != value)
        checkNotNull(updated)
        check(updated >= before.updated)
        val after = storage[id]
        checkNotNull(after)
        check(after.id == before.id)
        check(after.created == before.created)
        check(after.updated == updated)
        check(after.value == value)
        return after
    }

    fun storages(index: Int): SyncStorages {
        return stack.getOrElse(index = index) { error("No storages by index: $index!") }
    }

    fun <T : Comparable<T>> storage(index: Int, type: Class<T>): MutableStorage<T> {
        return storages(index = index)[type] ?: error("No storage($index/${type.name})!")
    }

    inline fun <reified T : Comparable<T>> storage(index: Int): MutableStorage<T> {
        return storages(index = index)[T::class.java] ?: error("No storage($index/${T::class.java.name})!")
    }

    inline fun <reified T : Comparable<T>> payload(storage: Int, index: Int): Payload<T> {
        return storage<T>(index = storage).payloads.getOrElse(index = index) {
            error("No payload by index $index in storage $storage/${T::class.java.name}!")
        }
    }

    internal inline fun <reified T : Comparable<T>> assertEquals(
        index: Int,
        expected: Collection<Payload<T>>,
    ) {
        assertEquals(
            expected = expected,
            actual = storage<T>(index).payloads,
            comparator = Comparators.payloads,
            assert = { _, e, a -> assertEquals(expected = e, actual = a) },
        )
    }
}
