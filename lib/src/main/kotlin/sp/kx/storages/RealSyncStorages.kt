package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.writeBytes
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.streamers.MutableFileStreamer
import sp.kx.streamers.MutableStreamer
import sp.kx.times.Times
import java.io.File
import java.util.UUID

class RealSyncStorages private constructor(
    private val dir: File,
    private val transformers: Map<UUID, CompositeTransformer<out Any>>,
    private val hashes: Hashes,
    private val times: Times,
    private val ids: Ids,
) : SyncStorages {
    class Builder {
        private val transformers = mutableMapOf<UUID, CompositeTransformer<out Any>>()

        fun <T : Any> add(id: UUID, type: Class<T>, transformer: Transformer<T>): Builder {
            if (transformers.containsKey(id)) error("ID \"$id\" is repeated!")
            transformers[id] = CompositeTransformer(type = type, delegate = transformer)
            return this
        }

        fun build(
            dir: File,
            hashes: Hashes,
            times: Times,
            ids: Ids,
        ): SyncStorages {
            if (transformers.isEmpty()) error("Empty storages!")
            // todo check dir
            return RealSyncStorages(
                dir = dir,
                transformers = transformers,
                hashes = hashes,
                times = times,
                ids = ids,
            )
        }
    }

    override fun <T : Any> get(type: Class<T>): MutableStorage<T>? {
        for ((id, it) in transformers) {
            val transformer = it.getTransformer(type = type) ?: continue
            val src = dir.resolve(id.toString())
            if (src.length() == 0L) {
                src.outputStream().use { stream ->
                    stream.writeBytes(0) // deleted
                    stream.writeBytes(0) // payloads
                }
            }
            return RealSyncStorage(
                id = id,
                streamer = MutableFileStreamer(src = src),
                transformer = transformer,
                hashes = hashes,
                times = times,
                ids = ids,
            )
        }
        return null
    }

    override fun getSyncStates(): SyncStates {
        val values = mutableMapOf<UUID, SyncState>()
        for ((id, _) in transformers) {
            val src = dir.resolve(id.toString())
            values[id] = RealSyncStorage.getSyncState(
                streamer = MutableFileStreamer(src = src),
                hashes = hashes,
            )
        }
        return SyncStates(values = values)
    }
}
