package sp.kx.storages

import java.util.UUID

data class SyncState(
    val valueStates: Map<UUID, ValueState>,
    val deleted: Set<UUID>,
)
