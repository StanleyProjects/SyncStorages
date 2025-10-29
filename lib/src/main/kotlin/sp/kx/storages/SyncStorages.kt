package sp.kx.storages

interface SyncStorages : MutableStorages {
    fun getSyncStates(): SyncStates
}
