package sp.kx.storages

import java.util.UUID

interface SyncStorages : MutableStorages {
    fun getSyncStates(): Map<UUID, SyncState>
    fun getMergeStates(syncStates: Map<UUID, SyncState>): Map<UUID, MergeState>
}
