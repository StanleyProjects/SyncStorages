package sp.kx.storages

import java.util.UUID
import kotlin.time.Duration

internal object Keys {
    val Strings = Storage.Key(UUID(0, 0), String::class.java)
    val Durations = Storage.Key(UUID(0, 1), Duration::class.java)
}
