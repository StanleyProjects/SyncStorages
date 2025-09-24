package sp.kx.storages

import java.util.UUID

class CommitState(
    val hash: ByteArray,
    val payloads: List<Payload<ByteArray>>,
    val deleted: Set<UUID>,
)
