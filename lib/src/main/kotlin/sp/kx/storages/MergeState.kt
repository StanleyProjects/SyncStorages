package sp.kx.storages

import java.util.UUID

class MergeState(
    val picks: Set<UUID>,
    val gives: List<Payload<ByteArray>>,
    val deleted: Set<UUID>,
) {
    override fun toString(): String {
        TODO("MergeState:toString")
    }

    override fun hashCode(): Int {
        TODO("MergeState:hashCode")
    }

    override fun equals(other: Any?): Boolean {
        TODO("MergeState:equals($other)")
    }
}
