package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.streamers.MutableStreamer
import sp.kx.times.Times
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class RealSyncStorage<T : Any>(
    override val id: UUID,
    private val streamer: MutableStreamer,
    private val transformer: Transformer<T>,
    private val hashes: Hashes,
    private val times: Times,
    private val ids: Ids,
) : SyncStorage<T> {
    init {
        val value = streamer.reader().use { it.read() }
        if (value < 0) streamer.writer().use {
            it.writeBytes(0) // deleted
            it.writeBytes(0) // items
        }
    }

    override val items: List<Payload<T>>
        get() {
            return streamer.reader().use { stream ->
                getItems(stream = stream, transformer = transformer)
            }
        }

    private val deleted: Set<UUID>
        get() {
            return streamer.reader().use { stream ->
                (0 until stream.readInt()).mapTo(HashSet()) { stream.readUUID() }
            }
        }

    private fun write(
        deleted: Set<UUID> = this.deleted,
        items: List<Payload<T>>,
    ) {
        streamer.writer().use { stream ->
            stream.writeBytes(deleted.size)
            deleted.forEach(stream::writeBytes)
            //
            stream.writeBytes(items.size)
            items.forEach { payload ->
                stream.writeBytes(payload.valueInfo.id)
                stream.writeBytes(payload.valueInfo.created.inWholeMilliseconds)
                stream.writeBytes(payload.valueState.updated.inWholeMilliseconds)
                val encoded = transformer.encode(payload.value)
                stream.writeBytes(encoded.size)
                stream.write(encoded)
            }
        }
    }

    private fun <U : Any> InputStream.readPayload(transformer: Transformer<U>): Payload<U> {
        val valueInfo = ValueInfo(
            id = readUUID(),
            created = readLong().milliseconds,
        )
        val updated = readLong().milliseconds
        val encoded = readBytes(readInt())
        return Payload(
            value = transformer.decode(encoded = encoded),
            valueInfo = valueInfo,
            valueState = ValueState(
                updated = updated,
                hash = hashes.map(encoded),
            ),
        )
    }

    private fun InputStream.readPayload(): Payload<ByteArray> {
        val valueInfo = ValueInfo(
            id = readUUID(),
            created = readLong().milliseconds,
        )
        val updated = readLong().milliseconds
        val encoded = readBytes(readInt())
        return Payload(
            value = encoded,
            valueInfo = valueInfo,
            valueState = ValueState(
                updated = updated,
                hash = hashes.map(encoded),
            ),
        )
    }

    private fun <U : Any> getItems(stream: InputStream, transformer: Transformer<U>): List<Payload<U>> {
        stream.skip((stream.readInt() * 16).toLong()) // deleted
        return (0 until stream.readInt()).map { index ->
            stream.readPayload(transformer = transformer)
        }
    }

    private fun getItems(stream: InputStream): List<Payload<ByteArray>> {
        stream.skip((stream.readInt() * 16).toLong()) // deleted
        return (0 until stream.readInt()).map { index ->
            stream.readPayload()
        }
    }

    override fun getSyncState(): SyncState {
        return streamer.reader().use { stream ->
            val deleted: Set<UUID> = (0 until stream.readInt()).mapTo(HashSet()) { stream.readUUID() }
            val valueStates = (0 until stream.readInt()).associate { index ->
                val id = stream.readUUID()
                stream.skip(8) // created
                val updated = stream.readLong().milliseconds
                val encoded = stream.readBytes(stream.readInt())
                id to ValueState(
                    updated = updated,
                    hash = hashes.map(encoded),
                )
            }
            SyncState(
                valueStates = valueStates,
                deleted = deleted,
            )
        }
    }

    override fun getMergeState(syncState: SyncState): MergeState {
        return streamer.reader().use { stream ->
            val downloaded = HashSet<UUID>()
            val encoded = mutableListOf<Payload<ByteArray>>()
            val deleted = deleted
            val items = getItems(stream = stream)
            for (payload in items) {
                if (syncState.valueStates.containsKey(payload.valueInfo.id)) continue
                if (syncState.deleted.contains(payload.valueInfo.id)) continue
                encoded.add(payload)
            }
            for ((id, valueState) in syncState.valueStates) {
                val payload = items.firstOrNull { it.valueInfo.id == id }
                if (payload == null) {
                    if (deleted.contains(id)) continue
                    downloaded.add(id)
                } else if (valueState.updated > payload.valueState.updated) {
                    downloaded.add(id)
                } else if (!valueState.hash.contentEquals(payload.valueState.hash)) {
                    encoded.add(payload)
                }
            }
            MergeState(
                downloaded = downloaded,
                encoded = encoded,
                deleted = deleted,
            )
        }
    }

    private fun bytesOf(payloads: List<Payload<out Any>>): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            payloads.forEach {
                stream.writeBytes(it.valueInfo.id)
                stream.writeBytes(it.valueState.updated.inWholeMilliseconds)
                stream.writeBytes(it.valueState.hash)
            }
            stream.toByteArray()
        }
    }

    private fun <U : Any> Payload<ByteArray>.map(transformer: Transformer<U>): Payload<U> {
        return Payload(
            value = transformer.decode(value),
            valueInfo = valueInfo,
            valueState = valueState,
        )
    }

    override fun merge(mergeState: MergeState): CommitState {
        val deleted = deleted
        val payloads = mutableListOf<Payload<T>>()
        val encoded = mutableListOf<Payload<ByteArray>>()
        for (item in streamer.reader().use(::getItems)) {
            if (mergeState.deleted.contains(item.valueInfo.id)) continue
            if (mergeState.encoded.any { it.valueInfo.id == item.valueInfo.id }) continue
            if (mergeState.downloaded.contains(item.valueInfo.id)) encoded.add(item)
            payloads += item.map(transformer)
        }
        for (item in mergeState.encoded) {
            payloads += item.map(transformer)
        }
        payloads.sortWith(Comparators.payloads)
        write(
            items = payloads,
            deleted = deleted + mergeState.deleted,
        )
        return CommitState(
            hash = hashes.map(bytesOf(payloads = payloads)),
            encoded = encoded,
            deleted = deleted,
        )
    }

    override fun commit(commitState: CommitState): Boolean {
        val payloads = mutableListOf<Payload<T>>()
        // todo no changes
        for (item in items) {
            if (commitState.deleted.contains(item.valueInfo.id)) continue
            if (commitState.encoded.any { it.valueInfo.id == item.valueInfo.id }) continue
            payloads += item
        }
        for (item in commitState.encoded) {
            payloads += item.map(transformer)
        }
        payloads.sortWith(Comparators.payloads)
        val hash = hashes.map(bytesOf(payloads = payloads))
        check(hash.contentEquals(commitState.hash)) { "Wrong hash!" }
        write(
            items = payloads,
            deleted = deleted + commitState.deleted,
        )
        return true
    }

    override fun add(value: T): Payload<T> {
        val created = times.now()
        val payload = Payload(
            value = value,
            valueInfo = ValueInfo(
                id = ids.random(),
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
                write(deleted = deleted + id, items = items)
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
                    updated = times.now(),
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
            stream.skip((stream.readInt() * 16).toLong()) // deleted
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
