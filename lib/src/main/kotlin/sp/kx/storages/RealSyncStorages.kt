package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.writeBytes
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.streamers.FileStreamer
import sp.kx.streamers.MutableFileStreamer
import sp.kx.times.Times
import java.io.File
import java.util.UUID

class RealSyncStorages private constructor(
    private val dir: File,
    private val holders: List<TransformerHolder<out Any>>,
    private val hashes: Hashes,
    private val times: Times,
    private val ids: Ids,
) : SyncStorages {
    private class TransformerHolder<T : Any>(
        val id: UUID,
        val transformer: Transformer<T>,
        val type: Class<out T>,
    )

    class Builder {
        private val holders = mutableListOf<TransformerHolder<out Any>>()

        fun <T : Any> add(id: UUID, type: Class<out T>, transformer: Transformer<T>): Builder {
            if (type.isPrimitive) TODO("RealSyncStorages:add(id: $id, type: $type)")
            if (holders.any { it.id == id }) error("ID \"$id\" is repeated!")
            val holder = TransformerHolder(id = id, transformer = transformer, type = type)
            holders.add(holder)
            return this
        }

        fun build(
            dir: File,
            hashes: Hashes,
            times: Times,
            ids: Ids,
        ): SyncStorages {
            if (holders.isEmpty()) error("Empty storages!")
            // todo check dir
            for (holder in holders) {
                val src = dir.resolve(holder.id.toString())
                if (src.length() == 0L) {
                    src.outputStream().use { stream ->
                        stream.writeBytes(0) // deleted
                        stream.writeBytes(0) // payloads
                    }
                }
            }
            return RealSyncStorages(
                dir = dir,
                holders = holders,
                hashes = hashes,
                times = times,
                ids = ids,
            )
        }
    }

    override fun <T : Any> get(type: Class<T>): MutableStorage<T>? {
        for (holder in holders) {
            if (!holder.type.isAssignableFrom(type)) continue
            val src = dir.resolve(holder.id.toString())
            return SyncStorage(
                id = holder.id,
                streamer = MutableFileStreamer(src = src),
                transformer = holder.transformer as Transformer<T>,
                times = times,
                ids = ids,
            )
        }
        return null
    }

    override fun getSyncStates(): Map<UUID, SyncState> {
        val syncStates = mutableMapOf<UUID, SyncState>()
        for (holder in holders) {
            val src = dir.resolve(holder.id.toString())
            syncStates[holder.id] = SyncStorageAlgorithms.getSyncState(
                streamer = FileStreamer(delegate = src),
                hashes = hashes,
            )
        }
        return syncStates
    }

    override fun getMergeStates(syncStates: Map<UUID, SyncState>): Map<UUID, MergeState> {
        val mergeStates = mutableMapOf<UUID, MergeState>()
        for ((id, syncState) in syncStates) {
            if (holders.none { it.id == id }) error("No storage by ID: \"$id\"!")
            val src = dir.resolve(id.toString())
            mergeStates[id] = SyncStorageAlgorithms.getMergeState(
                streamer = FileStreamer(delegate = src),
                hashes = hashes,
                syncState = syncState,
            )
        }
        return mergeStates
    }

    override fun merge(mergeStates: Map<UUID, MergeState>): Map<UUID, CommitState> {
        val commitStates = mutableMapOf<UUID, CommitState>()
        for ((id, mergeState) in mergeStates) {
            if (holders.none { it.id == id }) error("No storage by ID: \"$id\"!")
            val src = dir.resolve(id.toString())
            commitStates[id] = SyncStorageAlgorithms.merge(
                streamer = MutableFileStreamer(src = src),
                hashes = hashes,
                mergeState = mergeState,
            )
        }
        return commitStates
    }

    override fun commit(commitStates: Map<UUID, CommitState>): Set<UUID> {
        val result = mutableSetOf<UUID>()
        for ((id, commitState) in commitStates) {
            if (holders.none { it.id == id }) error("No storage by ID: \"$id\"!")
            val src = dir.resolve(id.toString())
            val commited = SyncStorageAlgorithms.commit(
                streamer = MutableFileStreamer(src = src),
                hashes = hashes,
                commitState = commitState,
            )
            if (commited) result.add(id)
        }
        return result
    }
}
