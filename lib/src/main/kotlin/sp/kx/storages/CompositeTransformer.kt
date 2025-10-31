package sp.kx.storages

import sp.kx.bytes.Transformer

internal class CompositeTransformer<T : Any>(
    val type: Class<out T>,
    val delegate: Transformer<T>,
) {
    override fun toString(): String {
        return "CompositeTransformer(type: $type)"
    }
}
