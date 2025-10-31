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
import sp.kx.streamers.Streamer
import sp.kx.times.Times
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStorage<T : Any>(
    override val id: UUID,
    private val streamer: MutableStreamer,
    private val transformer: Transformer<T>,
    private val hashes: Hashes,
    private val times: Times,
    private val ids: Ids,
) : MutableStorage<T> {
    override val payloads: List<Payload<T>>
        get() {
            return streamer.reader().use { stream ->
                stream.skip((stream.readInt() * 16).toLong()) // deleted
                (0 until stream.readInt()).map { index ->
                    readPayload(stream = stream, hashes = hashes, transformer = transformer)
                }
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
        payloads: List<Payload<T>>,
    ) {
        streamer.writer().use { stream ->
            write(
                stream = stream,
                deleted = deleted,
                payloads = payloads,
                transformer = transformer,
            )
        }
    }

    fun commit(commitState: CommitState): Boolean {
        return commit(
            streamer = streamer,
            hashes = hashes,
            transformer = transformer,
            commitState = commitState,
        )
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
        write(payloads = payloads + payload)
        return payload
    }

    override fun delete(id: UUID): Boolean {
        val payloads = payloads.toMutableList()
        for (index in payloads.indices) {
            val it = payloads[index]
            if (it.valueInfo.id == id) {
                payloads.removeAt(index)
                write(deleted = deleted + id, payloads = payloads)
                return true
            }
        }
        return false
    }

    override fun update(id: UUID, value: T): ValueState? {
        val payloads = payloads.toMutableList()
        for (index in payloads.indices) {
            val it = payloads[index]
            if (it.valueInfo.id == id) {
                payloads.removeAt(index)
                val valueState = ValueState(
                    updated = times.now(),
                    hash = hashes.map(transformer.encode(value)),
                )
                val payload = Payload(
                    value = value,
                    valueInfo = it.valueInfo,
                    valueState = valueState,
                )
                write(payloads = payloads + payload)
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

    companion object {
        private fun <T : Any> Payload<ByteArray>.map(transformer: Transformer<T>): Payload<T> {
            return Payload(
                value = transformer.decode(value),
                valueInfo = valueInfo,
                valueState = valueState,
            )
        }

        private fun bytesOf(payloads: List<Payload<out Any>>): ByteArray {
            return ByteArrayOutputStream().use { stream ->
                payloads.forEach { payload ->
                    stream.writeBytes(payload.valueInfo.id)
                    stream.writeBytes(payload.valueState.updated.inWholeMilliseconds)
                    stream.writeBytes(payload.valueState.hash)
                }
                stream.toByteArray()
            }
        }

        private fun <T : Any> write(
            stream: OutputStream,
            deleted: Set<UUID>,
            payloads: List<Payload<out T>>,
            transformer: Transformer<in T>,
        ) {
            stream.writeBytes(deleted.size)
            deleted.forEach(stream::writeBytes)
            //
            stream.writeBytes(payloads.size)
            payloads.forEach { payload ->
                stream.writeBytes(payload.valueInfo.id)
                stream.writeBytes(payload.valueInfo.created.inWholeMilliseconds)
                stream.writeBytes(payload.valueState.updated.inWholeMilliseconds)
                val encoded = transformer.encode(payload.value)
                stream.writeBytes(encoded.size)
                stream.write(encoded)
            }
        }

        private fun readPayload(stream: InputStream, hashes: Hashes): Payload<ByteArray> {
            val valueInfo = ValueInfo(
                id = stream.readUUID(),
                created = stream.readLong().milliseconds,
            )
            val updated = stream.readLong().milliseconds
            val encoded = stream.readBytes(stream.readInt())
            return Payload(
                value = encoded,
                valueInfo = valueInfo,
                valueState = ValueState(
                    updated = updated,
                    hash = hashes.map(encoded),
                ),
            )
        }

        private fun <U : Any> readPayload(stream: InputStream, hashes: Hashes, transformer: Transformer<U>): Payload<U> {
            val valueInfo = ValueInfo(
                id = stream.readUUID(),
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

        fun getSyncState(streamer: Streamer, hashes: Hashes): SyncState {
            val deleted = HashSet<UUID>()
            return streamer.reader().use { stream ->
                (0 until stream.readInt()).forEach { _ ->
                    deleted.add(stream.readUUID())
                }
                val valueStates = (0 until stream.readInt()).associate { _ ->
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

        fun getMergeState(
            streamer: Streamer,
            hashes: Hashes,
            syncState: SyncState,
        ): MergeState {
            val deleted = HashSet<UUID>()
            val locals: List<Payload<ByteArray>>
            streamer.reader().use { stream ->
                (0 until stream.readInt()).forEach { _ ->
                    deleted.add(stream.readUUID())
                }
                locals = (0 until stream.readInt()).map { _ ->
                    readPayload(stream = stream, hashes = hashes)
                }
            }
            val picks = HashSet<UUID>()
            val gives = mutableListOf<Payload<ByteArray>>()
            for (payload in locals) {
                if (syncState.valueStates.containsKey(payload.valueInfo.id)) continue
                if (syncState.deleted.contains(payload.valueInfo.id)) continue
                gives.add(payload)
            }
            for ((id, valueState) in syncState.valueStates) {
                val payload = locals.firstOrNull { it.valueInfo.id == id }
                if (payload == null) {
                    if (deleted.contains(id)) continue
                    picks.add(id)
                } else if (valueState.updated > payload.valueState.updated) {
                    picks.add(id)
                } else if (!valueState.hash.contentEquals(payload.valueState.hash)) {
                    gives.add(payload)
                }
            }
            return MergeState(
                deleted = deleted,
                picks = picks,
                gives = gives,
            )
        }

        fun <T : Any> merge(
            streamer: MutableStreamer,
            hashes: Hashes,
            transformer: Transformer<T>,
            mergeState: MergeState,
        ): CommitState {
            val deleted = HashSet<UUID>()
            val locals: List<Payload<ByteArray>>
            streamer.reader().use { stream ->
                (0 until stream.readInt()).forEach { _ ->
                    deleted.add(stream.readUUID())
                }
                locals = (0 until stream.readInt()).map { _ ->
                    readPayload(stream = stream, hashes = hashes)
                }
            }
            val payloads = mutableListOf<Payload<T>>()
            val gives = mutableListOf<Payload<ByteArray>>()
            for (payload in locals) {
                if (mergeState.deleted.contains(payload.valueInfo.id)) continue
                if (mergeState.gives.any { it.valueInfo.id == payload.valueInfo.id }) continue
                if (mergeState.picks.contains(payload.valueInfo.id)) gives.add(payload)
                payloads.add(payload.map(transformer))
            }
            for (payload in mergeState.gives) {
                payloads.add(payload.map(transformer))
            }
            payloads.sortWith(Comparators.payloads)
            deleted.addAll(mergeState.deleted)
            streamer.writer().use { stream ->
                write(
                    stream = stream,
                    deleted = deleted,
                    payloads = payloads,
                    transformer = transformer,
                )
            }
            return CommitState(
                hash = hashes.map(bytesOf(payloads = payloads)),
                gives = gives,
                deleted = deleted,
            )
        }

        fun <T : Any> commit(
            streamer: MutableStreamer,
            hashes: Hashes,
            transformer: Transformer<T>,
            commitState: CommitState,
        ): Boolean {
            val deleted = HashSet<UUID>()
            val payloads = mutableListOf<Payload<T>>()
            // todo no changes
            streamer.reader().use { stream ->
                (0 until stream.readInt()).forEach { _ ->
                    deleted.add(stream.readUUID())
                }
                val locals = (0 until stream.readInt()).map { _ ->
                    readPayload(stream = stream, hashes = hashes, transformer = transformer)
                }
                for (payload in locals) {
                    if (commitState.deleted.contains(payload.valueInfo.id)) continue
                    if (commitState.gives.any { it.valueInfo.id == payload.valueInfo.id }) continue
                    payloads.add(payload)
                }
                for (payload in commitState.gives) {
                    payloads.add(payload.map(transformer))
                }
            }
            payloads.sortWith(Comparators.payloads)
            val hash = hashes.map(bytesOf(payloads = payloads))
            check(hash.contentEquals(commitState.hash)) { "Wrong hash!" }
            streamer.writer().use { stream ->
                write(
                    stream = stream,
                    deleted = deleted + commitState.deleted,
                    payloads = payloads,
                    transformer = transformer,
                )
            }
            return true
        }
    }
}
