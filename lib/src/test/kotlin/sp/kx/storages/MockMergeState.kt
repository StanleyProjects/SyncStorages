package sp.kx.storages

import java.util.UUID

internal fun mockMergeState(
    deleted: Set<UUID> = emptySet(),
    picks: Set<UUID> = emptySet(),
    gives: List<Payload<ByteArray>> = emptyList(),
): MergeState {
    return MergeState(
        deleted = deleted,
        picks = picks,
        gives = gives,
    )
}
