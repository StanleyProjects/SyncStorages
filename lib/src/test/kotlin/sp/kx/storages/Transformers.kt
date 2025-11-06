package sp.kx.storages

import sp.kx.bytes.Transformer
import sp.kx.bytes.readLong
import sp.kx.bytes.toByteArray
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal object Transformers {
    object Strings : Transformer<String> {
        override fun decode(encoded: ByteArray): String {
            return String(encoded)
        }

        override fun encode(decoded: String): ByteArray {
            return decoded.toByteArray()
        }
    }

    object Durations : Transformer<Duration> {
        override fun decode(encoded: ByteArray): Duration {
            return encoded.readLong().milliseconds
        }

        override fun encode(decoded: Duration): ByteArray {
            return decoded.inWholeMilliseconds.toByteArray()
        }
    }

    fun <T : Any> get(type: Class<out T>): Transformer<T> {
        return when {
            String::class.java.isAssignableFrom(type) -> Strings
            Duration::class.java.isAssignableFrom(type) -> Durations
            else -> error("Type $type is not supported!")
        } as Transformer<T>
    }

    fun <T : Any> value(type: Class<out T>, salt: Int): T {
        return when {
            String::class.java.isAssignableFrom(type) -> "value:$salt"
            Duration::class.java.isAssignableFrom(type) -> (1_000_000 + salt).milliseconds
            else -> error("Type $type is not supported!")
        } as T
    }

    inline fun <reified T : Comparable<T>> map(payload: Payload<out T>): Payload<ByteArray> {
        return Payload(
            id = payload.id,
            created = payload.created,
            updated = payload.updated,
            value = get(T::class.java).encode(decoded = payload.value),
        )
    }
}
