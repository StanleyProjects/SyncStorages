package sp.kx.storages

internal object Comparators {
    val payloads = object : Comparator<Payload<out Any>> {
        override fun compare(
            p0: Payload<out Any>?,
            p1: Payload<out Any>?,
        ): Int {
            if (p0 == null) {
                if (p1 == null) return 0
                return -1
            }
            if (p1 == null) return 1
            if (p0.valueInfo.created > p1.valueInfo.created) return 1
            if (p0.valueInfo.created < p1.valueInfo.created) return -1
            return p0.valueInfo.id.compareTo(p1.valueInfo.id)
        }
    }
}
