package sp.service.sample

import sp.kx.storages.RealSyncStorage
import sp.kx.storages.SyncStorage
import java.util.UUID

fun main() {
    val storage: SyncStorage<String> = RealSyncStorage(
        id = UUID.randomUUID(),
    )
    println("storage: ${storage.id}")
    check(storage.items.isEmpty())
    TODO()
}
