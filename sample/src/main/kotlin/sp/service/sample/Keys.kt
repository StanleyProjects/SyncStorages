package sp.service.sample

import sp.kx.storages.Storage
import java.util.UUID
import kotlin.time.Duration

internal object Keys {
    val Strings = Storage.Key(UUID(0, 0), String::class.java)
    val Durations = Storage.Key(UUID(0, 0), Duration::class.java)
}
