package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readInt
import sp.kx.bytes.toByteArray

internal object IntTransformer : Transformer<Int> {
    override fun decode(encoded: ByteArray): Int {
        return encoded.readInt()
    }

    override fun encode(decoded: Int): ByteArray {
        return decoded.toByteArray()
    }
}
