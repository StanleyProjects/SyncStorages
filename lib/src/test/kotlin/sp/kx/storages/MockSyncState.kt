package sp.kx.storages

import java.util.UUID

internal fun mockSyncState(
    valueStates: Map<UUID, ValueState> = emptyMap(),
    deleted: Set<UUID> = emptySet(),
): SyncState {
    return SyncState(
        valueStates = valueStates,
        deleted = deleted,
    )
}
