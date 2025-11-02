package sp.kx.storages

import kotlin.time.Duration

internal fun mockValueState(
    updated: Duration = Duration.ZERO,
    hash: ByteArray = byteArrayOf(4, 3, 2, 1),
): ValueState {
    return ValueState(
        updated = updated,
        hash = hash,
    )
}
