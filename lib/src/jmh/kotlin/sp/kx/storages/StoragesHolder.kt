package sp.kx.storages

internal class StoragesHolder<T : Any>(val storages: MutableStorages, type: Class<T>) {
    val storage = storages[type] ?: error("No storage!")
    val payloads = storage.payloads
    val first = payloads[0]
    val mid = payloads[payloads.size / 2]
    val last = payloads.lastOrNull() ?: error("No payload!")
}
