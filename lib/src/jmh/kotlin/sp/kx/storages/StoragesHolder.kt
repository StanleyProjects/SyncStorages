package sp.kx.storages

import java.util.UUID

internal class StoragesHolder<T : Any>(val storages: MutableStorages, key: Storage.Key<T>) {
    val storage = storages[key] ?: error("No storage!")
    val payloads = storage.payloads
    val first = payloads[0]
    val mid = payloads[payloads.size / 2]
    val last = payloads.lastOrNull() ?: error("No payload!")
    val random = payloads.randomOrNull() ?: error("No payload!")
    val none: UUID

    init {
        var bits = 0L
        while (true) {
            val id = UUID(0, bits)
            if (payloads.none { it.id == id }) {
                none = id
                break
            }
            bits++
        }
    }
}
