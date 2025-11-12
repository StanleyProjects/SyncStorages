package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readBytes
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.writeBytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds

internal object FooTransformer : Transformer<Foo> {
    override fun decode(encoded: ByteArray): Foo {
        return ByteArrayInputStream(encoded).use { stream ->
            val number = stream.readInt()
            val size = stream.readInt()
            if (size < 0) error("Size error!")
            val bytes = stream.readBytes(size = size)
            val text = String(bytes)
            val time = stream.readLong().milliseconds
            Foo(
                number = number,
                text = text,
                time = time,
            )
        }
    }

    override fun encode(decoded: Foo): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            stream.writeBytes(decoded.number)
            val bytes = decoded.text.toByteArray()
            stream.writeBytes(bytes.size)
            stream.writeBytes(bytes)
            stream.writeBytes(decoded.time.inWholeMilliseconds)
            stream.toByteArray()
        }
    }

    fun value(index: Int): Foo {
        return Foo(
            number = index,
            text = "text:$index",
            time = index.milliseconds,
        )
    }
}
