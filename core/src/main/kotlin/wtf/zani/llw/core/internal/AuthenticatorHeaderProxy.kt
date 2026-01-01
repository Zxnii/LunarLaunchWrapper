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

fun <K, V> create(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V): Map<K, V> =
    process(k1 to v1, k2 to v2, k3 to v3, k4 to v4)

fun <K, V> create(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V, k5: K, v5: V): Map<K, V> =
    process(k1 to v1, k2 to v2, k3 to v3, k4 to v4, k5 to v5)

fun <K, V> create(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V, k5: K, v5: V, k6: K, v6: V): Map<K, V> =
    process(k1 to v1, k2 to v2, k3 to v3, k4 to v4, k5 to v5, k6 to v6)

fun <K, V> create(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V, k5: K, v5: V, k6: K, v6: V, k7: K, v7: V): Map<K, V> =
    process(k1 to v1, k2 to v2, k3 to v3, k4 to v4, k5 to v5, k6 to v6, k7 to v7)

fun <K, V> create(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V, k5: K, v5: V, k6: K, v6: V, k7: K, v7: V, k8: K, v8: V): Map<K, V> =
    process(k1 to v1, k2 to v2, k3 to v3, k4 to v4, k5 to v5, k6 to v6, k7 to v7, k8 to v8)

fun <K, V> create(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V, k5: K, v5: V, k6: K, v6: V, k7: K, v7: V, k8: K, v8: V, k9: K, v9: V): Map<K, V> =
    process(k1 to v1, k2 to v2, k3 to v3, k4 to v4, k5 to v5, k6 to v6, k7 to v7, k8 to v8, k9 to v9)

fun <K, V> create(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V, k5: K, v5: V, k6: K, v6: V, k7: K, v7: V, k8: K, v8: V, k9: K, v9: V, k10: K, v10: V): Map<K, V> =
    process(k1 to v1, k2 to v2, k3 to v3, k4 to v4, k5 to v5, k6 to v6, k7 to v7, k8 to v8, k9 to v9, k10 to v10)
    