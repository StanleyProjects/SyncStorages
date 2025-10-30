package sp.kx.storages

import java.util.UUID

internal fun mockCommitState(
    hash: ByteArray = byteArrayOf(4, 3, 2, 1),
    gives: List<Payload<ByteArray>> = emptyList(),
    deleted: Set<UUID> = emptySet(),
): CommitState {
    return CommitState(
        hash = hash,
        gives = gives,
        deleted = deleted,
    )
}
