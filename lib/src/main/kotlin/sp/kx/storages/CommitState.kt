package sp.kx.storages

import java.util.Objects
import java.util.UUID

class CommitState(
    val deleted: Set<UUID>,
    val gives: List<Payload<ByteArray>>,
    val hash: ByteArray,
) {
    override fun toString(): String {
        return "CommitState(deleted: $deleted, gives: ${gives.size}, hash: ${hash.size})"
    }

    override fun hashCode(): Int {
        return Objects.hash(
            deleted.fold(1) { acc, it -> 31 * acc + it.hashCode() },
            gives.fold(1) { acc, it -> 31 * acc + Payload.hashCode(it) },
            hash.contentHashCode(),
        )
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is CommitState -> {
                return deleted == other.deleted &&
                    gives.size == other.gives.size &&
                    gives.indices.all { index ->
                        Payload.equals(expected = gives[index], actual = other.gives[index])
                    } && hash.contentEquals(other.hash)
            }
            else -> false
        }
    }
}
