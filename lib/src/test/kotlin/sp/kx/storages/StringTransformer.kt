package sp.kx.storages

import sp.kx.bytes.Transformer

internal object StringTransformer : Transformer<String> {
    override fun decode(encoded: ByteArray): String {
        return String(encoded)
    }

    override fun encode(decoded: String): ByteArray {
        return decoded.toByteArray()
    }
}
