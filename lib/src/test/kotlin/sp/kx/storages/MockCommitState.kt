package sp.kx.storages

import java.util.UUID

internal fun mockCommitState(
    deleted: Set<UUID> = emptySet(),
    gives: List<Payload<ByteArray>> = emptyList(),
    hash: ByteArray = byteArrayOf(4, 3, 2, 1),
): CommitState {
    return CommitState(
        deleted = deleted,
        gives = gives,
        hash = hash,
    )
}
