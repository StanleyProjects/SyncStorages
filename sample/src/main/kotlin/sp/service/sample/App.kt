package sp.service.sample

import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.RealIds
import sp.kx.storages.RealSyncStorage
import sp.kx.storages.SyncStorage
import sp.kx.streamers.MutableFileStreamer
import sp.kx.times.RealTimes
import java.io.File
import java.util.UUID

fun main() {
    val transformer = object : Transformer<String> {
        override fun decode(encoded: ByteArray): String {
            return String(encoded)
        }

        override fun encode(decoded: String): ByteArray {
            return decoded.toByteArray()
        }
    }
    val storage: SyncStorage<String> = RealSyncStorage(
        id = UUID.randomUUID(),
        streamer = MutableFileStreamer(File.createTempFile("foo", "bar")),
        transformer = transformer,
        hashes = Hashes.MD5,
        times = RealTimes(),
        ids = RealIds(),
    )
    println("storage: ${storage.id}")
    check(storage.payloads.isEmpty())
    //
    val p0 = storage.add("foo")
    println("item: ${p0.valueInfo}")
    check(storage.payloads.size == 1)
    check(storage[p0.valueInfo.id]!!.value == "foo")
    val p1 = storage.add("bar")
    println("item: ${p1.valueInfo}")
    check(storage.payloads.size == 2)
    check(storage[p1.valueInfo.id]!!.value == "bar")
    val p2 = storage.add("baz")
    println("item: ${p2.valueInfo}")
    check(storage.payloads.size == 3)
    check(storage[p2.valueInfo.id]!!.value == "baz")
    //
    val v1 = storage.update(id = p1.valueInfo.id, value = "qux")
    checkNotNull(v1)
    check(storage.payloads.size == 3)
    check(storage[p1.valueInfo.id]!!.value == "qux")
    //
    check(storage.delete(p2.valueInfo.id))
    check(storage.payloads.size == 2)
    check(storage[p0.valueInfo.id]!!.value == "foo")
    check(storage[p1.valueInfo.id]!!.value == "qux")
}
