package sp.kx.storages

import sp.kx.bytes.Transformer

internal class CompositeTransformer<T : Any>(
    private val type: Class<out T>,
    val delegate: Transformer<T>,
) {
    fun <U : Any> getTransformer(type: Class<U>): Transformer<U>? {
        if (type.isAssignableFrom(this.type)) return delegate as Transformer<U>
        return null
    }
}
