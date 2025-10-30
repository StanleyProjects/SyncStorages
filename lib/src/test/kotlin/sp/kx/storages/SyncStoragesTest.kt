package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.io.File
import java.util.HexFormat

internal class SyncStoragesTest {
    @Test
    fun getSyncStatesTest(@TempDir dir: File) {
        val transformer: Transformer<String> = StringTransformer
        val hashes: Hashes = Hashes.MD5
        val times: Times = MockTimes()
        val ids: Ids = MockIds()
        //
        val storages: SyncStorages = RealSyncStorages.Builder()
            .add(id = ids.random(), type = String::class.java, transformer = transformer)
            .build(
                dir = dir,
                hashes = hashes,
                times = times,
                ids = ids,
            )
        //
        val storage = storages[String::class.java] ?: error("No storage!")
        val value = "foo bar baz"
        val payload = storage.add(value = value)
        //
        val syncStates = storages.getSyncStates()
        assertEquals(
            expected = mapOf(
                storage.id to mockSyncState(
                    valueStates = mapOf(
                        payload.valueInfo.id to mockValueState(
                            hash = HexFormat.of().parseHex("ab07acbb1e496801937adfa772424bf7"),
                        ),
                    ),
                ),
            ),
            actual = syncStates,
            assert = ::assertEquals,
        )
    }
}
