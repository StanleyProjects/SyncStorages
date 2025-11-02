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

    fun add(index: Int, value: String): Payload<String> {
        val storage = storage<String>(index = index)
        val payload = storage.add(value = value)
        check(payload.value == value)
        check(payload.created == payload.updated)
        return payload
    }

    fun add(index: Int, value: Int): Payload<Int> {
        val storage = storage<Int>(index = index)
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

    fun storages(index: Int): MutableStorages {
        return stack.getOrElse(index) { error("No storages by index: $index!") }
    }

    inline fun <reified T : Any> storage(index: Int): MutableStorage<T> {
        return storages(index = index)[T::class.java] ?: error("No storage($index/${T::class.java.name})!")
    }
}
