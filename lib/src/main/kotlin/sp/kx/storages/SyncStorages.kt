package sp.kx.storages

import java.util.UUID

interface SyncStorages : MutableStorages {
    fun getSyncStates(): Map<UUID, SyncState>
    fun getMergeStates(syncStates: Map<UUID, SyncState>): Map<UUID, MergeState>
    fun merge(mergeStates: Map<UUID, MergeState>): Map<UUID, CommitState>
    fun commit(commitStates: Map<UUID, CommitState>): Set<UUID>
}
