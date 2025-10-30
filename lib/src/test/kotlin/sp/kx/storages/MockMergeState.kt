package sp.kx.storages

import java.util.UUID

internal fun mockMergeState(
    downloaded: Set<UUID> = emptySet(),
    encoded: List<Payload<ByteArray>> = emptyList(),
    deleted: Set<UUID> = emptySet(),
): MergeState {
    return MergeState(
        downloaded = downloaded,
        encoded = encoded,
        deleted = deleted,
    )
}
