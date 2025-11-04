package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import sp.kx.bytes.hex

internal inline fun <reified T : Any> assertEquals(
    expected: Collection<T>,
    actual: Collection<T>,
    comparator: Comparator<in T>,
    assert: (index: Int, expected: T, actual: T) -> Unit,
    message: String = T::class.java.name,
) {
    assertEquals(expected.size, actual.size, message)
    val sorted = actual.sortedWith(comparator)
    expected.sortedWith(comparator).forEachIndexed { index, e ->
        assert(index, e, sorted[index])
    }
}

internal inline fun <reified T : Comparable<T>> assertEquals(expected: Collection<T>, actual: Collection<T>, message: String = T::class.java.name) {
    assertEquals(expected.size, actual.size, message)
    val sorted = actual.sorted()
    expected.sorted().forEachIndexed { index, e ->
        assertEquals(e, sorted[index], "index: $index")
    }
}

internal fun <K : Comparable<K>, V : Any> assertEquals(
    expected: Map<K, V>,
    actual: Map<K, V>,
    assert: (index: Int, expected: V, actual: V) -> Unit,
) {
    assertEquals(expected.size, actual.size)
    val sorted = actual.entries.sortedBy { (it, _) -> it }
    expected.entries.sortedBy { (it, _) -> it }.forEachIndexed { index, (key, value) ->
        val entry = sorted[index]
        assertEquals(key, entry.key)
        assert(index, value, entry.value)
    }
}

internal fun Payload<ByteArray>.assertEquals(actual: Payload<ByteArray>, message: String) {
    assertEquals(id, actual.id, "Payload:id")
    assertEquals(created, actual.created, "Payload:created")
    assertEquals(updated, actual.updated, "Payload:updated")
    assertEquals(value.size, actual.value.size, message)
    assertTrue(value.contentEquals(actual.value), message)
}

internal fun <T : Comparable<T>> assertEquals(expected: Payload<out T>, actual: Payload<out T>) {
    assertEquals(expected.id, actual.id)
    assertEquals(expected.created, actual.created)
    assertEquals(expected.updated, actual.updated)
    assertEquals(expected.value, actual.value)
}

internal fun ValueState.assertEquals(actual: ValueState) {
    assertEquals(updated, actual.updated)
    val message = """
        ValueState:hash:
        expected: ${hash.hex()}
        actual:   ${actual.hash.hex()}
    """.trimIndent()
    assertTrue(hash.contentEquals(actual.hash), message)
    assertEquals(this, actual, message)
}

internal fun SyncState.assertEquals(actual: SyncState) {
    assertEquals(expected = deleted, actual = actual.deleted, message = "SyncState:deleted")
    assertEquals(
        expected = valueStates,
        actual = actual.valueStates,
        assert = { _, e, a -> e.assertEquals(actual = a) },
    )
    assertEquals(this, actual)
}

internal fun MergeState.assertEquals(actual: MergeState) {
    assertEquals(expected = deleted, actual = actual.deleted, message = "MergeState:deleted")
    assertEquals(expected = picks, actual = actual.picks, message = "MergeState:picks")
    assertEquals(
        expected = gives,
        actual = actual.gives,
        comparator = Comparators.payloads,
        assert = { index, e, a -> e.assertEquals(actual = a, message = "MergeState:gives[$index]") },
    )
    assertEquals(this, actual)
}

internal fun CommitState.assertEquals(actual: CommitState) {
    assertEquals(expected = deleted, actual = actual.deleted, message = "CommitState:deleted")
    assertEquals(
        expected = gives,
        actual = actual.gives,
        comparator = Comparators.payloads,
        assert = { index, e, a -> e.assertEquals(actual = a, message = "CommitState:gives[$index]") },
    )
    val message = """
        CommitState:hash:
        expected: ${hash.hex()}
        actual:   ${actual.hash.hex()}
    """.trimIndent()
    assertTrue(hash.contentEquals(actual.hash), message)
    assertEquals(this, actual)
}
