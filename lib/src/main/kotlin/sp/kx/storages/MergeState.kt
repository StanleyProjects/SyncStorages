package sp.kx.storages

import java.util.Objects
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
            gives.fold(1) { acc, it -> 31 * acc + Payload.hashCode(it) },
        )
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is MergeState -> {
                return deleted == other.deleted &&
                    picks == other.picks &&
                    gives.size == other.gives.size &&
                    gives.indices.all { index ->
                        Payload.equals(expected = gives[index], actual = other.gives[index])
                    }
            }
            else -> false
        }
    }
}
