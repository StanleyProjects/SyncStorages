package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.hashes.HashFunction
import sp.kx.streamers.MutableStreamer
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class RealSyncStorage<T : Any>(
    override val id: UUID,
    private val streamer: MutableStreamer,
    private val transformer: Transformer<T>,
    private val hf: HashFunction,
) : SyncStorage<T> {
    override val items: List<Payload<T>>
        get() {
            return streamer.reader().use { stream ->
                (0 until stream.readInt()).map { index ->
                    val valueInfo = ValueInfo(
                        id = stream.readUUID(),
                        created = stream.readLong().milliseconds,
                    )
                    val updated = stream.readLong().milliseconds
                    val encoded = stream.readBytes(stream.readInt())
                    Payload(
                        value = transformer.decode(encoded = encoded),
                        valueInfo = valueInfo,
                        valueState = ValueState(
                            updated = updated,
                            hash = hf.map(encoded),
                        ),
                    )
                }
            }
        }

    override val syncState: SyncState
        get() = TODO("Not yet implemented")

    override fun getMergeState(syncState: SyncState): MergeState {
        TODO("getMergeState")
    }

    override fun merge(mergeState: MergeState): CommitState {
        TODO("merge")
    }

    override fun commit(commitState: CommitState): Boolean {
        TODO("commit")
    }

    override fun add(value: T): Payload<T> {
        TODO("add")
    }

    override fun delete(id: UUID): Boolean {
        TODO("delete")
    }

    override fun set(id: UUID, value: T): ValueState? {
        TODO("set")
    }

    override fun get(id: UUID): Payload<T>? {
        TODO("get")
    }
}
