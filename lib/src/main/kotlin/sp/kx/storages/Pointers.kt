package sp.kx.storages

import sp.kx.bytes.readInt
import sp.kx.bytes.readUUID
import sp.kx.bytes.writeBytes
import sp.kx.streamers.FileStreamer
import sp.kx.streamers.Streamer
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal object Pointers {
    fun getStreamer(stream: InputStream, dir: File, id: UUID): Streamer {
        val pointer = getPointer(stream = stream, id = id) ?: 0
        val file = getFile(dir = dir, id = id, pointer = pointer)
        return FileStreamer(file)
    }

    fun getFile(stream: InputStream, dir: File, id: UUID): File {
        val pointer = getPointer(stream = stream, id = id) ?: 0
        return getFile(dir = dir, id = id, pointer = pointer)
    }

    private fun getFile(dir: File, id: UUID, pointer: Int): File {
        return dir.resolve("$id-$pointer.bin")
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

    fun setPointers(stream: OutputStream, ids: Map<UUID, Int>) {
        val entries = ids.entries
        stream.writeBytes(entries.size)
        for ((id, pointer) in entries) {
            stream.writeBytes(id)
            stream.writeBytes(pointer)
        }
    }
}
