package sp.kx.storages

import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import sp.kx.hashes.Hashes
import sp.kx.streamers.Streamer
import java.io.OutputStream
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal object SyncStorageAlgorithms {
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

    fun getSyncState(streamer: Streamer, hashes: Hashes): SyncState {
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
}
