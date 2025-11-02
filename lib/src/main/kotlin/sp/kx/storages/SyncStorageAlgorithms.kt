package sp.kx.storages

import sp.kx.bytes.writeBytes
import java.io.OutputStream
import java.util.UUID

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
}
