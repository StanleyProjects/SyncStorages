package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

internal inline fun <reified T : Any> assertEquals(
    expected: Collection<T>,
    actual: Collection<T>,
    comparator: Comparator<in T>,
    assert: (index: Int, expected: T, actual: T) -> Unit,
) {
    assertEquals(expected.size, actual.size, T::class.java.name)
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

internal fun assertEquals(expected: Payload<ByteArray>, actual: Payload<ByteArray>, message: String) {
    assertEquals(expected.value.size, actual.value.size, message)
    assertTrue(expected.value.contentEquals(actual.value), message)
    assertEquals(expected.valueInfo, actual.valueInfo, message)
    assertEquals(expected.valueState, actual.valueState, message)
}

internal fun <T : Comparable<T>> assertEquals(expected: Payload<out T>, actual: Payload<out T>) {
    assertEquals(expected.value, actual.value)
    assertEquals(expected.valueInfo, actual.valueInfo)
    assertEquals(expected.valueState, actual.valueState)
}
