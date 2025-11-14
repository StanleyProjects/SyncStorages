package sp.service.sample

import sp.kx.bytes.Transformer
import sp.kx.hashes.Hashes
import sp.kx.ids.RealIds
import sp.kx.storages.RealSyncStorages
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
    val hashes = Hashes.MD5
    val times = RealTimes()
    val ids = RealIds()
    val storages = (0 until 2).map { index ->
        val files = File("/tmp/storages-$index")
        if (files.exists()) {
            check(files.isDirectory)
            check(files.deleteRecursively())
        }
        check(files.mkdir())
        RealSyncStorages.Builder()
            .add(id = UUID(0, 0), type = String::class.java, transformer = transformer)
            .build(
                files = files,
                hashes = hashes,
                times = times,
                ids = ids,
            )
    }
    storages.indices.forEach { index ->
        val storage = storages[index][String::class.java] ?: error("No storage!")
        val values = (0..9).map { "value:$index:$it" }
        storage.addAll(values = values)
    }
    storages[0].commit(storages[1].merge(storages[0].getMergeStates(storages[1].getSyncStates())))
    val payloads = storages.indices.map { index ->
        val storage = storages[index][String::class.java] ?: error("No storage!")
        storage.payloads
    }
    for (index in payloads[0].indices) {
        val expected = payloads[0][index]
        val actual = payloads[1][index]
        check(expected.id == actual.id)
        check(expected.created == actual.created)
        check(expected.updated == actual.updated)
        check(expected.value == actual.value)
    }
}
