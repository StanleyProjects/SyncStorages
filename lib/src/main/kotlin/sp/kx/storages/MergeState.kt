package sp.kx.storages

import java.util.UUID

class MergeState(
    val downloaded: Set<UUID>,
    val payloads: List<Payload<ByteArray>>,
    val deleted: Set<UUID>,
)
