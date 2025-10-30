package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readInt
import sp.kx.bytes.toByteArray

internal object Transformers {
    object Ints : Transformer<Int> {
        override fun decode(encoded: ByteArray): Int {
            return encoded.readInt()
        }

        override fun encode(decoded: Int): ByteArray {
            return decoded.toByteArray()
        }
    }

    object Strings : Transformer<String> {
        override fun decode(encoded: ByteArray): String {
            return String(encoded)
        }

        override fun encode(decoded: String): ByteArray {
            return decoded.toByteArray()
        }
    }
}
