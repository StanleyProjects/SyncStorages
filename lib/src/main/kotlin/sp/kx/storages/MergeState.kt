package sp.kx.storages

import java.util.Objects
import java.util.TreeSet
import java.util.UUID

class MergeState(
    val deleted: Set<UUID>,
    val picks: Set<UUID>,
    val gives: List<Payload<ByteArray>>,
) {
    override fun toString(): String {
        return "MergeState(deleted: $deleted, picks: $picks, gives: ${gives.size})"
    }

    override fun hashCode(): Int {
        return Objects.hash(
            deleted.fold(1) { acc, it -> 31 * acc + it.hashCode() },
            picks.fold(1) { acc, it -> 31 * acc + it.hashCode() },
            gives.fold(1) { acc, it -> 31 * acc + TODO("MergeState:hashCode($it)") },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other !is MergeState) return false
        if (TreeSet(deleted) != TreeSet(other.deleted)) {
            return false
        }
        if (TreeSet(picks) != TreeSet(other.picks)) {
            return false
        }
        if (gives.size != other.gives.size) {
            return false
        }
        TODO("MergeState:equals($other)")
        return true
    }
}
