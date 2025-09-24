package sp.kx.storages

import java.util.UUID

class RealSyncStorage<T : Any> : SyncStorage<T> {
    override val id: UUID
        get() = TODO("Not yet implemented")

    override val items: List<Payload<T>>
        get() = TODO("Not yet implemented")

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
