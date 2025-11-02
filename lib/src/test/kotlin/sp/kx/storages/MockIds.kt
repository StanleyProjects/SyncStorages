package sp.kx.storages

import sp.kx.ids.Ids
import java.util.UUID

internal class MockIds(
    private var leastSigBits: Long = 0,
) : Ids {
    override fun random(): UUID {
        return UUID(0, leastSigBits++)
    }
}
