package sp.kx.storages

import sp.kx.times.Times
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class MockTimes(
    private var ms: Long = 0,
) : Times {
    override fun now(): Duration {
        return ms++.milliseconds
    }
}
