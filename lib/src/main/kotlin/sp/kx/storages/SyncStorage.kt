package sp.kx.storages

interface SyncStorage<T : Any> : MutableStorage<T> {
    fun getSyncState(): SyncState
    fun getMergeState(syncState: SyncState): MergeState
    fun merge(mergeState: MergeState): CommitState
    fun commit(commitState: CommitState): Boolean
}
