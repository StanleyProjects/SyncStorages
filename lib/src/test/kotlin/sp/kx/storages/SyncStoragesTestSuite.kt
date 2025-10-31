package sp.kx.storages

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

    private fun <T : Any> RealSyncStorages.Builder.add(
        id: UUID,
        ct: CompositeTransformer<T>,
    ): RealSyncStorages.Builder {
        return add(id = id, type = ct.type, transformer = ct.delegate)
    }

    constructor(
        dir: File,
        hashes: Hashes = Hashes.MD5,
        times: Times = MockTimes(),
        ids: Ids = MockIds(),
        count: Int = 3,
        transformers: List<CompositeTransformer<out Any>> = listOf(
            CompositeTransformer(String::class.java, Transformers.Strings),
            CompositeTransformer(Int::class.java, Transformers.Ints),
        ),
    ) {
        this.hashes = hashes
        this.times = times
        this.ids = ids
        this.stack = (0 until count).map { index ->
            val builder = RealSyncStorages.Builder()
            transformers.forEachIndexed { ci, ct ->
                builder.add(id = UUID(ci.toLong(), 0), ct = ct)
            }
            builder.build(
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

    fun put(index: Int, value: String): Payload<String> {
        val storage = require<String>(index = index)
        val payload = storage.add(value = value)
        check(payload.value == value)
        check(payload.valueInfo.created == payload.valueState.updated)
        check(payload.valueState.hash.contentEquals(hashes.map(Transformers.Strings.encode(value))))
        return payload
    }

    fun put(index: Int, value: Int): Payload<Int> {
        val storage = require<Int>(index = index)
        val payload = storage.add(value = value)
        check(payload.value == value)
        check(payload.valueInfo.created == payload.valueState.updated)
        check(payload.valueState.hash.contentEquals(hashes.map(Transformers.Ints.encode(value))))
        return payload
    }

    inline fun <reified T : Any> require(index: Int): MutableStorage<T> {
        return stack[index][T::class.java] ?: error("No storage($index/${T::class.java.name})!")
    }
}
