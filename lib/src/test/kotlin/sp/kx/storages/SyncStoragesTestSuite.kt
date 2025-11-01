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
        val storage = require<String>(index = index)
        val payload = storage.add(value = value)
        check(payload.value == value)
        check(payload.valueInfo.created == payload.valueState.updated)
        check(payload.valueState.hash.contentEquals(hashes.map(Transformers.Strings.encode(value))))
        return payload
    }

    fun add(index: Int, value: Int): Payload<Int> {
        val storage = require<Int>(index = index)
        val payload = storage.add(value = value)
        check(payload.value == value)
        check(payload.valueInfo.created == payload.valueState.updated)
        check(payload.valueState.hash.contentEquals(hashes.map(Transformers.Ints.encode(value))))
        return payload
    }

    fun update(index: Int, id: UUID, value: String): ValueState? {
        val storage = require<String>(index = index)
        val before = storage[id]
        val valueState = storage.update(id = id, value = value)
        if (before == null) {
            check(valueState == null)
            return null
        }
        check(before.value != value)
        checkNotNull(valueState)
        check(valueState.updated >= before.valueState.updated)
        check(!valueState.hash.contentEquals(before.valueState.hash))
        check(valueState.hash.contentEquals(hashes.map(Transformers.Strings.encode(value))))
        val after = storage[id]
        checkNotNull(after)
        check(after.value == value)
        check(after.valueInfo == before.valueInfo)
        check(after.valueState == valueState)
        return valueState
    }

    inline fun <reified T : Any> require(index: Int): MutableStorage<T> {
        return stack[index][T::class.java] ?: error("No storage($index/${T::class.java.name})!")
    }
}
