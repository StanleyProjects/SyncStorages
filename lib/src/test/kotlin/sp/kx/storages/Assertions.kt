package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

internal fun <T : Any> assertEquals(
    expected: Collection<T>,
    actual: Collection<T>,
    comparator: Comparator<T>,
    assert: (index: Int, expected: T, actual: T) -> Unit,
) {
    assertEquals(expected.size, actual.size)
    val sorted = actual.sortedWith(comparator)
    expected.sortedWith(comparator).forEachIndexed { index, e ->
        assert(index, e, sorted[index])
    }
}

internal fun <T : Comparable<T>> assertEquals(expected: Collection<T>, actual: Collection<T>) {
    assertEquals(expected.size, actual.size)
    val sorted = actual.sorted()
    expected.sorted().forEachIndexed { index, e ->
        assertEquals(e, sorted[index])
    }
}

internal fun <K : Comparable<K>, V : Any> assertEquals(
    expected: Map<K, V>,
    actual: Map<K, V>,
    assert: (expected: V, actual: V) -> Unit,
) {
    assertEquals(expected.size, actual.size)
    val sorted = actual.entries.sortedBy { (it, _) -> it }
    expected.entries.sortedBy { (it, _) -> it }.forEachIndexed { index, (key, value) ->
        val entry = sorted[index]
        assertEquals(key, entry.key)
        assert(value, entry.value)
    }
}

internal fun <T : Any> assertEquals(expected: Payload<out T>, actual: Payload<out T>, message: String = "${expected.valueInfo.id}") {
    assertEquals(expected.value, actual.value, message)
    assertEquals(expected.valueInfo, actual.valueInfo, message)
    assertEquals(expected.valueState.updated, actual.valueState.updated, message)
    assertTrue(expected.valueState.hash.contentEquals(actual.valueState.hash), message)
}
