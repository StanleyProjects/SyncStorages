package sp.service.sample

import sp.kx.bytes.Transformer
import sp.kx.hashes.HashFunction
import sp.kx.storages.RealSyncStorage
import sp.kx.storages.SyncStorage
import sp.kx.streamers.MutableFileStreamer
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
        hf = HashFunction.MD5,
    )
    println("storage: ${storage.id}")
    check(storage.items.isEmpty())
    TODO()
}
