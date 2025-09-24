package sp.kx.storages

interface SyncStorage<T : Any> : MutableStorage<T> {
    val syncState: SyncState

    fun getMergeState(syncState: SyncState): MergeState
    fun merge(mergeState: MergeState): CommitState
    fun commit(commitState: CommitState): Boolean
}
