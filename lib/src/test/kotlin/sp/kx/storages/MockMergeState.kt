package sp.kx.storages

import java.util.UUID

internal fun mockMergeState(
    picks: Set<UUID> = emptySet(),
    gives: List<Payload<ByteArray>> = emptyList(),
    deleted: Set<UUID> = emptySet(),
): MergeState {
    return MergeState(
        picks = picks,
        gives = gives,
        deleted = deleted,
    )
}
