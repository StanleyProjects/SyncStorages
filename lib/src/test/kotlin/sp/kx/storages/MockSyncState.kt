package sp.kx.storages

import java.util.UUID

internal fun mockSyncState(
    deleted: Set<UUID> = emptySet(),
    valueStates: Map<UUID, ValueState> = emptyMap(),
): SyncState {
    return SyncState(
        deleted = deleted,
        valueStates = valueStates,
    )
}
