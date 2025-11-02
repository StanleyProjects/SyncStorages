package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readInt
import sp.kx.bytes.readUUID
import sp.kx.ids.Ids
import sp.kx.streamers.MutableStreamer
import sp.kx.times.Times
import java.util.UUID
import kotlin.time.Duration

internal class SyncStorage<T : Any>(
    override val id: UUID,
    private val streamer: MutableStreamer,
    private val transformer: Transformer<T>,
    private val times: Times,
    private val ids: Ids,
) : MutableStorage<T> {
    override val payloads: List<Payload<T>>
        get() {
            return streamer.reader().use { stream ->
                stream.skip((stream.readInt() * 16).toLong()) // deleted
                (0 until stream.readInt()).map { _ ->
                    val id = stream.readUUID()
                    SyncStorageAlgorithms.readPayload(
                        stream = stream,
                        id = id,
                        transformer = transformer,
                    )
                }
            }
        }

    override fun add(value: T): Payload<T> {
        val deleted = HashSet<UUID>()
        val payloads = ArrayList<Payload<ByteArray>>()
        streamer.reader().use { stream ->
            (0 until stream.readInt()).forEach { _ ->
                deleted.add(stream.readUUID())
            }
            (0 until stream.readInt()).forEach { _ ->
                payloads.add(SyncStorageAlgorithms.readPayload(stream = stream))
            }
        }
        val id = ids.random()
        val created = times.now()
        payloads += Payload(
            id = id,
            created = created,
            updated = created,
            value = transformer.encode(value),
        )
        streamer.writer().use { stream ->
            SyncStorageAlgorithms.write(
                stream = stream,
                deleted = deleted,
                payloads = payloads,
            )
        }
        return Payload(
            id = id,
            created = created,
            updated = created,
            value = value,
        )
    }

    override fun delete(id: UUID): Boolean {
        val deleted = HashSet<UUID>()
        val payloads = ArrayList<Payload<ByteArray>>()
        streamer.reader().use { stream ->
            (0 until stream.readInt()).forEach { _ ->
                deleted.add(stream.readUUID())
            }
            (0 until stream.readInt()).forEach { _ ->
                payloads.add(SyncStorageAlgorithms.readPayload(stream = stream))
            }
        }
        for (index in payloads.indices) {
            val it = payloads[index]
            if (it.id == id) {
                payloads.removeAt(index)
                deleted.add(id)
                streamer.writer().use { stream ->
                    SyncStorageAlgorithms.write(
                        stream = stream,
                        deleted = deleted,
                        payloads = payloads,
                    )
                }
                return true
            }
        }
        return false
    }

    override fun update(id: UUID, value: T): Duration? {
        val deleted = HashSet<UUID>()
        val payloads = ArrayList<Payload<ByteArray>>()
        streamer.reader().use { stream ->
            (0 until stream.readInt()).forEach { _ ->
                deleted.add(stream.readUUID())
            }
            (0 until stream.readInt()).forEach { _ ->
                payloads.add(SyncStorageAlgorithms.readPayload(stream = stream))
            }
        }
        for (index in payloads.indices) {
            val it = payloads[index]
            if (it.id == id) {
                payloads.removeAt(index)
                val updated = times.now()
                payloads += Payload(
                    id = it.id,
                    created = it.created,
                    updated = updated,
                    value = transformer.encode(value),
                )
                streamer.writer().use { stream ->
                    SyncStorageAlgorithms.write(
                        stream = stream,
                        deleted = deleted,
                        payloads = payloads,
                    )
                }
                return updated
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
                return SyncStorageAlgorithms.readPayload(
                    stream = stream,
                    id = id,
                    transformer = transformer,
                )
            }
        }
        return null
    }
}
