package sp.kx.storages

import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import sp.kx.hashes.Hashes
import sp.kx.streamers.Streamer
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
}
