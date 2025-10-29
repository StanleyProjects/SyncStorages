package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.streamers.MutableFileStreamer
import sp.kx.times.Times
import java.io.File
import java.util.UUID

class SyncStorages private constructor(
    private val dir: File,
    private val transformers: Map<UUID, CompositeTransformer<out Any>>,
    private val hashes: Hashes,
    private val times: Times,
    private val ids: Ids,
) : MutableStorages {
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
            return SyncStorages(
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
            val transformer = it.getTransformer(type) ?: continue
            return RealSyncStorage(
                id = id,
                streamer = MutableFileStreamer(src = dir.resolve(id.toString())),
                transformer = transformer,
                hashes = hashes,
                times = times,
                ids = ids,
            )
        }
        return null
    }
}
