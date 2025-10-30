package sp.kx.storages

import java.util.UUID

class MergeState(
    val picks: Set<UUID>,
    val gives: List<Payload<ByteArray>>,
    val deleted: Set<UUID>,
)
