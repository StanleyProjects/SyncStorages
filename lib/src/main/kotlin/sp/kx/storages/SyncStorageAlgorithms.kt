package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import sp.kx.hashes.Hashes
import sp.kx.streamers.MutableStreamer
import sp.kx.streamers.Streamer
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal object SyncStorageAlgorithms {
    fun readPayload(stream: InputStream): Payload<ByteArray> {
        return Payload(
            id = stream.readUUID(),
            created = stream.readLong().milliseconds,
            updated = stream.readLong().milliseconds,
            value = stream.readBytes(stream.readInt()),
        )
    }

    fun <T : Any> readPayload(stream: InputStream, transformer: Transformer<T>, id: UUID): Payload<T> {
        return Payload(
            id = id,
            created = stream.readLong().milliseconds,
            updated = stream.readLong().milliseconds,
            value = transformer.decode(stream.readBytes(stream.readInt())),
        )
    }

    fun write(
        stream: OutputStream,
        deleted: Set<UUID>,
        payloads: List<Payload<ByteArray>>,
    ) {
        stream.writeBytes(deleted.size)
        deleted.forEach(stream::writeBytes)
        stream.writeBytes(payloads.size)
        payloads.forEach { payload ->
            stream.writeBytes(payload.id)
            stream.writeBytes(payload.created.inWholeMilliseconds)
            stream.writeBytes(payload.updated.inWholeMilliseconds)
            stream.writeBytes(payload.value.size)
            stream.write(payload.value)
        }
    }

    private fun bytesOf(payloads: List<Payload<ByteArray>>, hashes: Hashes): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            payloads.forEach { payload ->
                stream.writeBytes(payload.id)
                stream.writeBytes(hashes.map(payload.value))
            }
            stream.toByteArray()
        }
    }

    fun getSyncState(
        streamer: Streamer,
        hashes: Hashes,
    ): SyncState {
        val deleted = HashSet<UUID>()
        val valueStates = HashMap<UUID, ValueState>()
        streamer.reader().use { stream ->
            (0 until stream.readInt()).forEach { _ ->
                deleted.add(stream.readUUID())
            }
            (0 until stream.readInt()).forEach { _ ->
                val id = stream.readUUID()
                stream.skip(8) // created
                val updated = stream.readLong().milliseconds
                val encoded = stream.readBytes(stream.readInt())
                valueStates[id] = ValueState(
                    updated = updated,
                    hash = hashes.map(encoded),
                )
            }
        }
        return SyncState(
            deleted = deleted,
            valueStates = valueStates,
        )
    }

    fun getMergeState(
        streamer: Streamer,
        hashes: Hashes,
        syncState: SyncState,
    ): MergeState {
        val deleted = HashSet<UUID>()
        val locals = ArrayList<Payload<ByteArray>>()
        streamer.reader().use { stream ->
            (0 until stream.readInt()).forEach { _ ->
                deleted.add(stream.readUUID())
            }
            (0 until stream.readInt()).forEach { _ ->
                locals.add(readPayload(stream = stream))
            }
        }
        val picks = HashSet<UUID>()
        val gives = ArrayList<Payload<ByteArray>>()
        for (payload in locals) {
            if (syncState.valueStates.containsKey(payload.id)) continue
            if (syncState.deleted.contains(payload.id)) continue
            gives.add(payload)
        }
        for ((id, valueState) in syncState.valueStates) {
            val payload = locals.firstOrNull { it.id == id }
            if (payload == null) {
                if (deleted.contains(id)) continue
                picks.add(id)
            } else if (valueState.updated > payload.updated) {
                picks.add(id)
            } else if (!valueState.hash.contentEquals(hashes.map(payload.value))) {
                gives.add(payload)
            }
        }
        return MergeState(
            deleted = deleted,
            picks = picks,
            gives = gives,
        )
    }

    fun merge(
        streamer: MutableStreamer,
        hashes: Hashes,
        mergeState: MergeState,
    ): CommitState {
        val deleted = HashSet<UUID>()
        val locals = ArrayList<Payload<ByteArray>>()
        streamer.reader().use { stream ->
            (0 until stream.readInt()).forEach { _ ->
                deleted.add(stream.readUUID())
            }
            (0 until stream.readInt()).forEach { _ ->
                locals.add(readPayload(stream = stream))
            }
        }
        val payloads = mutableListOf<Payload<ByteArray>>()
        val gives = mutableListOf<Payload<ByteArray>>()
        for (payload in locals) {
            if (mergeState.deleted.contains(payload.id)) continue
            if (mergeState.gives.any { it.id == payload.id }) continue
            if (mergeState.picks.contains(payload.id)) gives.add(payload)
            payloads.add(payload)
        }
        for (payload in mergeState.gives) {
            payloads.add(payload)
        }
        payloads.sortWith(Comparators.payloads)
        deleted.addAll(mergeState.deleted)
        streamer.writer().use { stream ->
            write(
                stream = stream,
                deleted = deleted,
                payloads = payloads,
            )
        }
        return CommitState(
            hash = hashes.map(bytesOf(payloads = payloads, hashes = hashes)),
            gives = gives,
            deleted = deleted,
        )
    }

    fun commit(
        streamer: MutableStreamer,
        hashes: Hashes,
        commitState: CommitState,
    ): Boolean {
        val deleted = HashSet<UUID>()
        val locals = ArrayList<Payload<ByteArray>>()
        // todo no changes
        streamer.reader().use { stream ->
            (0 until stream.readInt()).forEach { _ ->
                deleted.add(stream.readUUID())
            }
            (0 until stream.readInt()).forEach { _ ->
                locals.add(readPayload(stream = stream))
            }
        }
        val payloads = ArrayList<Payload<ByteArray>>()
        for (payload in locals) {
            if (commitState.deleted.contains(payload.id)) continue
            if (commitState.gives.any { it.id == payload.id }) continue
            payloads.add(payload)
        }
        payloads.addAll(commitState.gives)
        payloads.sortWith(Comparators.payloads)
        deleted.addAll(commitState.deleted)
        val hash = hashes.map(bytesOf(payloads = payloads, hashes = hashes))
        check(hash.contentEquals(commitState.hash)) { "Wrong hash!" }
        streamer.writer().use { stream ->
            write(
                stream = stream,
                deleted = deleted,
                payloads = payloads,
            )
        }
        return true
    }
}
