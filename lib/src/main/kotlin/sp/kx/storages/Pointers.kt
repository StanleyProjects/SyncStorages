package sp.kx.storages

import sp.kx.bytes.readInt
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal object Pointers {
    fun getFile(stream: InputStream, dir: File, id: UUID): File {
        val pointer = getPointer(stream = stream, id = id) ?: 0
        return getFile(dir = dir, id = id, pointer = pointer)
    }

    fun getFile(dir: File, id: UUID, pointer: Int): File {
        return dir.resolve(getName(id = id, pointer = pointer))
    }

    fun getName(id: UUID, pointer: Int): String {
        return "$id-$pointer.bin"
    }

    fun getPointer(stream: InputStream, id: UUID): Int? {
        for (index in 0 until stream.readInt()) {
            if (id == stream.readUUID()) {
                return stream.readInt()
            }
            stream.skip(4)
        }
        return null
    }

    fun writePointers(stream: InputStream, pointers: MutableMap<UUID, Int>) {
        for (index in 0 until stream.readInt()) {
            pointers[stream.readUUID()] = stream.readInt()
        }
    }

    fun setPointers(stream: OutputStream, pointers: Map<UUID, Int>) {
        val entries = pointers.entries
        stream.writeBytes(entries.size)
        for ((id, pointer) in entries) {
            stream.writeBytes(id)
            stream.writeBytes(pointer)
        }
    }
}
