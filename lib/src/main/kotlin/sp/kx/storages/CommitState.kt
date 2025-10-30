package sp.kx.storages

import java.util.UUID

class CommitState(
    val hash: ByteArray,
    val gives: List<Payload<ByteArray>>,
    val deleted: Set<UUID>,
)
