package sp.kx.storages

import sp.kx.bytes.readInt
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal object Pointers {
    fun getPointer(stream: InputStream, id: UUID): Int? {
        for (index in 0 until stream.readInt()) {
            if (id == stream.readUUID()) {
                return stream.readInt()
            }
            stream.skip(4)
        }
        return null
    }

    fun setPointers(stream: OutputStream, ids: Map<UUID, Int>) {
        val entries = ids.entries
        stream.writeBytes(entries.size)
        for ((id, pointer) in entries) {
            stream.writeBytes(id)
            stream.writeBytes(pointer)
        }
    }

    fun getFile(dir: File, id: UUID, pointer: Int): File {
        return dir.resolve("$id-$pointer")
    }
}
