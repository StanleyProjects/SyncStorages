package sp.kx.storages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.io.File
import java.util.HexFormat
import kotlin.time.Duration.Companion.milliseconds

internal class SyncStoragesTest {
    @Test
    fun getSyncStatesTest(@TempDir dir: File) {
        val hashes: Hashes = Hashes.MD5
        val times: Times = MockTimes()
        val ids: Ids = MockIds()
        //
        val storages: SyncStorages = RealSyncStorages.Builder()
            .add(id = ids.random(), type = String::class.java, transformer = StringTransformer)
            .add(id = ids.random(), type = Int::class.java, transformer = IntTransformer)
            .build(
                dir = dir,
                hashes = hashes,
                times = times,
                ids = ids,
            )
        //
        val s1 = storages[String::class.java] ?: error("No storage!")
        val p1 = s1.add(value = "foo bar baz")
        //
        val s2 = storages[Int::class.java] ?: error("No storage!")
        val p2 = s2.add(value = 42)
        //
        assertEquals(
            expected = mapOf(
                s1.id to mockSyncState(
                    valueStates = mapOf(
                        p1.valueInfo.id to mockValueState(
                            hash = HexFormat.of().parseHex("ab07acbb1e496801937adfa772424bf7"),
                            updated = 0.milliseconds,
                        ),
                    ),
                ),
                s2.id to mockSyncState(
                    valueStates = mapOf(
                        p2.valueInfo.id to mockValueState(
                            hash = HexFormat.of().parseHex("a515855799ddbda08bc99fc2ce87fa79"),
                            updated = 1.milliseconds,
                        ),
                    ),
                ),
            ),
            actual = storages.getSyncStates(),
            assert = ::assertEquals,
        )
    }
}
