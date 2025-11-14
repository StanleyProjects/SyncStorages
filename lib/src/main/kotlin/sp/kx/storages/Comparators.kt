package sp.kx.storages

internal object Comparators {
    val payloads = object : Comparator<Payload<Any>> {
        override fun compare(
            p0: Payload<Any>?,
            p1: Payload<Any>?,
        ): Int {
            if (p0 == null) {
                if (p1 == null) return 0
                return -1
            }
            if (p1 == null) return 1
            if (p0.created > p1.created) return 1
            if (p0.created < p1.created) return -1
            return p0.id.compareTo(p1.id)
        }
    }
}
