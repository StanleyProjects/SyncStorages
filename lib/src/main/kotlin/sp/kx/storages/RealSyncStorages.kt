package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readInt
import sp.kx.bytes.writeBytes
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.streamers.FileStreamer
import sp.kx.streamers.MutableFileStreamer
import sp.kx.times.Times
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import kotlin.time.Duration

class RealSyncStorages private constructor(
    private val dir: File,
    private val holders: List<TransformerHolder<out Any>>,
    private val hashes: Hashes,
    private val times: Times,
    private val ids: Ids,
) : SyncStorages {
    private class TransformerHolder<T : Any>(
        val key: Storage.Key<T>,
        val transformer: Transformer<T>,
    )

    class Builder {
        private val holders = mutableListOf<TransformerHolder<out Any>>()

        fun <T : Any> add(key: Storage.Key<T>, transformer: Transformer<T>): Builder {
            if (key.type.isPrimitive) TODO("RealSyncStorages:add(id: ${key.id}, type: ${key.type})")
            if (holders.any { it.key.id == key.id }) error("Key(${key.type.name}) \"${key.id}\" is repeated!")
            val holder = TransformerHolder(key = key, transformer = transformer)
            holders.add(holder)
            return this
        }

        fun build(
            files: File,
            hashes: Hashes,
            times: Times,
            ids: Ids,
        ): SyncStorages {
            if (holders.isEmpty()) error("Empty storages!")
            if (files.exists()) {
                check(files.isDirectory)
            }
            val dir = files.resolve("storages")
            if (dir.exists()) {
                check(dir.isDirectory)
            } else {
                check(dir.mkdirs())
            }
            val pointers = dir.resolve("pointers.bin")
            if (pointers.exists()) {
                check(pointers.isFile)
            } else {
                pointers.createNewFile()
            }
            for (holder in holders) {
                val src = pointers.inputStream().use { stream ->
                    Pointers.getFile(stream = stream, dir = dir, id = holder.key.id)
                }
                if (src.exists()) {
                    check(src.isFile)
                } else {
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

    private val pointers: File

    init {
        check(dir.exists())
        check(dir.isDirectory)
        pointers = dir.resolve("pointers.bin")
        if (pointers.exists()) {
            check(pointers.isFile)
        } else {
            pointers.createNewFile()
        }
    }

    override fun <T : Any> get(key: Storage.Key<T>): MutableStorage<T>? {
        for (holder in holders) {
            if (holder.key != key) continue
            val src = dir.resolve("pointers.bin").inputStream().use { stream ->
                Pointers.getFile(stream = stream, dir = dir, id = holder.key.id)
            }
            return SyncStorage(
                id = holder.key.id,
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
            val src = dir.resolve("pointers.bin").inputStream().use { stream ->
                Pointers.getFile(stream = stream, dir = dir, id = holder.key.id)
            }
            syncStates[holder.key.id] = SyncStorageAlgorithms.getSyncState(
                streamer = FileStreamer(src),
                hashes = hashes,
            )
        }
        return syncStates
    }

    override fun getMergeStates(syncStates: Map<UUID, SyncState>): Map<UUID, MergeState> {
        val mergeStates = mutableMapOf<UUID, MergeState>()
        for ((id, syncState) in syncStates) {
            if (holders.none { it.key.id == id }) error("No storage by ID: \"$id\"!")
            val src = dir.resolve("pointers.bin").inputStream().use { stream ->
                Pointers.getFile(stream = stream, dir = dir, id = id)
            }
            mergeStates[id] = SyncStorageAlgorithms.getMergeState(
                streamer = FileStreamer(src),
                hashes = hashes,
                syncState = syncState,
            )
        }
        return mergeStates
    }

    override fun merge(mergeStates: Map<UUID, MergeState>): Map<UUID, CommitState> {
        val commitStates = mutableMapOf<UUID, CommitState>()
        val pointers = mutableMapOf<UUID, Int>()
        for (holder in holders) pointers[holder.key.id] = 0
        dir.resolve("pointers.bin").inputStream().use { stream ->
            Pointers.writePointers(stream = stream, pointers = pointers)
        }
        for ((id, mergeState) in mergeStates) {
            if (holders.none { it.key.id == id }) error("No storage by ID: \"$id\"!")
            val pointer = pointers[id] ?: 0
            val src = Pointers.getFile(dir = dir, id = id, pointer = pointer)
            val dst = Pointers.getFile(dir = dir, id = id, pointer = pointer + 1)
            commitStates[id] = SyncStorageAlgorithms.merge(
                streamer = MutableFileStreamer(src = src, dst = dst),
                hashes = hashes,
                mergeState = mergeState,
            )
            pointers[id] = pointer + 1
        }
        dir.resolve("pointers.bin").outputStream().use { stream ->
            Pointers.setPointers(stream = stream, pointers = pointers)
        }
        for (file in dir.listFiles()!!) {
            if (!file.exists() || !file.isFile) continue
            if (file.name == "pointers.bin") continue
            val contains = pointers.any { (id, pointer) ->
                file.name == Pointers.getName(id = id, pointer = pointer)
            }
            if (!contains) check(file.delete())
        }
        return commitStates
    }

    override fun commit(commitStates: Map<UUID, CommitState>): Set<UUID> {
        val result = mutableSetOf<UUID>()
        val pointers = mutableMapOf<UUID, Int>()
        for (holder in holders) pointers[holder.key.id] = 0
        dir.resolve("pointers.bin").inputStream().use { stream ->
            Pointers.writePointers(stream = stream, pointers = pointers)
        }
        for ((id, commitState) in commitStates) {
            if (holders.none { it.key.id == id }) error("No storage by ID: \"$id\"!")
            val pointer = pointers[id] ?: 0
            val src = Pointers.getFile(dir = dir, id = id, pointer = pointer)
            val dst = Pointers.getFile(dir = dir, id = id, pointer = pointer + 1)
            val commited = SyncStorageAlgorithms.commit(
                streamer = MutableFileStreamer(src = src, dst = dst),
                hashes = hashes,
                commitState = commitState,
            )
            if (commited) {
                result.add(id)
                pointers[id] = pointer + 1
            }
        }
        dir.resolve("pointers.bin").outputStream().use { stream ->
            Pointers.setPointers(stream = stream, pointers = pointers)
        }
        for (file in dir.listFiles()!!) {
            if (!file.exists() || !file.isFile) continue
            if (file.name == "pointers.bin") continue
            val contains = pointers.any { (id, pointer) ->
                file.name == Pointers.getName(id = id, pointer = pointer)
            }
            if (!contains) check(file.delete())
        }
        return result
    }

    private fun <T : Any> encode(
        holder: TransformerHolder<T>,
        operation: Transaction.Operation.Add<*>,
    ): ByteArray? {
        if (holder.key != operation.key) return null
        return holder.transformer.encode(operation.value as T)
    }

    private fun <T : Any> encode(
        holder: TransformerHolder<T>,
        operation: Transaction.Operation.Update<*>,
    ): ByteArray? {
        if (holder.key != operation.key) return null
        return holder.transformer.encode(operation.value as T)
    }

    override fun commit(transaction: Transaction) {
        val locals = HashMap<UUID, MutableList<Payload<ByteArray>>>()
        val gives = HashMap<UUID, MutableList<Payload<ByteArray>>>()
        val deleted = HashMap<UUID, MutableSet<UUID>>()
        val updated = HashSet<UUID>()
        val now = times.now()
        for (operation in transaction.operations) {
            when (operation) {
                is Transaction.Operation.Add<*> -> {
                    for (holder in holders) {
                        val value = encode(holder = holder, operation = operation) ?: continue
                        gives.getOrPut(holder.key.id, ::ArrayList) += Payload(
                            id = ids.random(),
                            created = now,
                            updated = now,
                            value = value,
                        )
                        updated.add(holder.key.id)
                        break
                    }
                }
                is Transaction.Operation.Delete<*> -> {
                    for (holder in holders) {
                        if (holder.key != operation.key) continue
                        val payloads = locals.getOrPut(holder.key.id) {
                            val src = dir.resolve("pointers.bin").inputStream().use { stream ->
                                Pointers.getFile(stream = stream, dir = dir, id = holder.key.id)
                            }
                            val payloads = ArrayList<Payload<ByteArray>>()
                            FileInputStream(src).use { stream ->
                                stream.skip((stream.readInt() * 16).toLong()) // deleted
                                (0 until stream.readInt()).forEach { _ ->
                                    payloads.add(SyncStorageAlgorithms.readPayload(stream = stream))
                                }
                            }
                            payloads
                        }
                        if (payloads.any { it.id == operation.id }) {
                            deleted.getOrPut(holder.key.id, ::HashSet).add(operation.id)
                            updated.add(holder.key.id)
                        }
                        break
                    }
                }
                is Transaction.Operation.DeleteFirst<*> -> TODO("RealSyncStorages:commit($transaction)")
                is Transaction.Operation.Update<*> -> {
                    for (holder in holders) {
                        val value = encode(holder = holder, operation = operation) ?: continue
                        val payloads = locals.getOrPut(holder.key.id) {
                            val src = dir.resolve("pointers.bin").inputStream().use { stream ->
                                Pointers.getFile(stream = stream, dir = dir, id = holder.key.id)
                            }
                            val payloads = ArrayList<Payload<ByteArray>>()
                            FileInputStream(src).use { stream ->
                                stream.skip((stream.readInt() * 16).toLong()) // deleted
                                (0 until stream.readInt()).forEach { _ ->
                                    payloads.add(SyncStorageAlgorithms.readPayload(stream = stream))
                                }
                            }
                            payloads
                        }
                        val payload = payloads.firstOrNull { it.id == operation.id } ?: continue
                        gives.getOrPut(holder.key.id, ::ArrayList) += Payload(
                            id = payload.id,
                            created = payload.created,
                            updated = now,
                            value = value,
                        )
                        updated.add(holder.key.id)
                        break
                    }
                }
                is Transaction.Operation.UpdateFirst<*> -> TODO("RealSyncStorages:commit($transaction)")
            }
        }
        val mergeStates = HashMap<UUID, MergeState>()
        for (id in updated) {
            mergeStates[id] = MergeState(
                deleted = deleted[id].orEmpty(),
                picks = emptySet(), // todo
                gives = gives[id].orEmpty(),
            )
        }
        merge(mergeStates = mergeStates)
    }
}
