package sp.kx.storages

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.Ids
import sp.kx.times.Times
import java.io.File
import java.util.HexFormat
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

internal class MutableStoragesTest {
    @Test
    fun getTest(@TempDir dir: File) {
        val transformer: Transformer<String> = StringTransformer
        val hashes: Hashes = Hashes.MD5
        val times: Times = MockTimes()
        val ids: Ids = MockIds()
        //
        val storages: MutableStorages = RealSyncStorages.Builder()
            .add(id = UUID(0, 0), type = String::class.java, transformer = transformer)
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
        val actual = storage[payload.valueInfo.id]
        checkNotNull(actual)
        assertEquals(expected = payload, actual = actual)
    }

    @Test
    fun addTest(@TempDir dir: File) {
        val transformer: Transformer<String> = StringTransformer
        val hashes: Hashes = Hashes.MD5
        val times: Times = MockTimes()
        val ids: Ids = MockIds()
        //
        val storages: MutableStorages = RealSyncStorages.Builder()
            .add(id = UUID(0, 0), type = String::class.java, transformer = transformer)
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
        assertEquals(
            expected = Payload(
                value = value,
                valueInfo = ValueInfo(
                    id = UUID(0, 0),
                    created = 0.milliseconds,
                ),
                valueState = ValueState(
                    updated = 0.milliseconds,
                    hash = HexFormat.of().parseHex("ab07acbb1e496801937adfa772424bf7"),
                ),
            ),
            actual = payload,
        )
    }
}
