package sp.kx.storages

import java.util.UUID

data class SyncStates(
    val values: Map<UUID, SyncState>,
)
