package wtf.zani.llw.core.internal

private fun <K, V> process(vararg entries: Pair<K, V>): Map<K, V> {
    val hasUserAgent = entries.any { e ->
        val k = e.first
        if (k !is String) return@any false
        
        k.lowercase() == "user-agent"
    }
    
    return if (hasUserAgent) {
        mapOf(*entries)
    } else {
        @Suppress("UNCHECKED_CAST")
        mapOf(
            *entries,
            "User-Agent" as K to "Lunar Client v2.20.4-2529" as V
        )
    }
}

fun <K, V> create(k1: K, v1: V): Map<K, V> =
    process(k1 to v1)

fun <K, V> create(k1: K, v1: V, k2: K, v2: V): Map<K, V> =
    process(k1 to v1, k2 to v2)

fun <K, V> create(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V): Map<K, V> =
    process(k1 to v1, k2 to v2, k3 to v3)
    