package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import sp.kx.hashes.Hashes
import sp.kx.streamers.MutableStreamer
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class RealSyncStorage<T : Any>(
    override val id: UUID,
    private val streamer: MutableStreamer,
    private val transformer: Transformer<T>,
    private val hashes: Hashes,
) : SyncStorage<T> {
    override val items: List<Payload<T>>
        get() {
            return streamer.reader().use { stream ->
                (0 until stream.readInt()).map { index ->
                    val valueInfo = ValueInfo(
                        id = stream.readUUID(),
                        created = stream.readLong().milliseconds,
                    )
                    val updated = stream.readLong().milliseconds
                    val encoded = stream.readBytes(stream.readInt())
                    Payload(
                        value = transformer.decode(encoded = encoded),
                        valueInfo = valueInfo,
                        valueState = ValueState(
                            updated = updated,
                            hash = hashes.map(encoded),
                        ),
                    )
                }
            }
        }

    override val syncState: SyncState
        get() = TODO("Not yet implemented")

    private fun write(items: List<Payload<T>>) {
        streamer.writer().use { stream ->
            stream.writeBytes(items.size)
            items.forEachIndexed { index, payload ->
                stream.writeBytes(payload.valueInfo.id)
                stream.writeBytes(payload.valueInfo.created.inWholeMilliseconds)
                stream.writeBytes(payload.valueState.updated.inWholeMilliseconds)
                val encoded = transformer.encode(payload.value)
                stream.writeBytes(encoded.size)
                stream.write(encoded)
            }
        }
    }

    override fun getMergeState(syncState: SyncState): MergeState {
        TODO("getMergeState")
    }

    override fun merge(mergeState: MergeState): CommitState {
        TODO("merge")
    }

    override fun commit(commitState: CommitState): Boolean {
        TODO("commit")
    }

    override fun add(value: T): Payload<T> {
        val created = System.currentTimeMillis().milliseconds // todo
        val payload = Payload(
            value = value,
            valueInfo = ValueInfo(
                id = UUID.randomUUID(), // todo
                created = created,
            ),
            valueState = ValueState(
                updated = created,
                hash = hashes.map(transformer.encode(value)),
            ),
        )
        write(items = items + payload)
        return payload
    }

    override fun delete(id: UUID): Boolean {
        val items = items.toMutableList()
        for (index in items.indices) {
            val it = items[index]
            if (it.valueInfo.id == id) {
                items.removeAt(index)
                write(items = items)
                return true
            }
        }
        return false
    }

    override fun set(id: UUID, value: T): ValueState? {
        val items = items.toMutableList()
        for (index in items.indices) {
            val it = items[index]
            if (it.valueInfo.id == id) {
                items.removeAt(index)
                val valueState = ValueState(
                    updated = System.currentTimeMillis().milliseconds, // todo
                    hash = hashes.map(transformer.encode(value)),
                )
                val payload = Payload(
                    value = value,
                    valueInfo = it.valueInfo,
                    valueState = valueState,
                )
                write(items = items + payload)
                return valueState
            }
        }
        return null
    }

    override fun get(id: UUID): Payload<T>? {
        streamer.reader().use { stream ->
            for (index in 0 until stream.readInt()) {
                if (id != stream.readUUID()) {
                    stream.skip(16)
                    stream.skip(stream.readInt().toLong())
                    continue
                }
                val valueInfo = ValueInfo(
                    id = id,
                    created = stream.readLong().milliseconds,
                )
                val updated = stream.readLong().milliseconds
                val encoded = stream.readBytes(stream.readInt())
                return Payload(
                    value = transformer.decode(encoded = encoded),
                    valueInfo = valueInfo,
                    valueState = ValueState(
                        updated = updated,
                        hash = hashes.map(encoded),
                    ),
                )
            }
        }
        return null
    }
}
