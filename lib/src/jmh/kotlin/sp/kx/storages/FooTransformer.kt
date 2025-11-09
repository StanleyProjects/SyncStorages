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
            Foo(
                number = stream.readInt(),
                text = String(stream.readBytes(stream.readInt())),
                time = stream.readLong().milliseconds,
            )
        }
    }

    override fun encode(decoded: Foo): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            stream.writeBytes(decoded.number)
            stream.writeBytes(decoded.text.length)
            stream.writeBytes(decoded.text.toByteArray())
            stream.writeBytes(decoded.time.inWholeMilliseconds)
            stream.toByteArray()
        }
    }
}
