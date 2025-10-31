package sp.kx.storages

import java.util.UUID

data class SyncState(
    val deleted: Set<UUID>,
    val valueStates: Map<UUID, ValueState>,
)
