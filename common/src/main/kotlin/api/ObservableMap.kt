package dev.voroscsoki.stratopolis.common.api

class ObservableMap<K,V>(
    private val innerMap: MutableMap<K,V> = mutableMapOf()
) : MutableMap<K,V> by innerMap {
    val listeners = mutableListOf<(MapChange<K,V>) -> Unit>()

    private fun notifyAll(change: MapChange<K,V>) {
        listeners.forEach { it(change) }
    }

    override fun put(key: K, value: V): V? {
        val oldValue = innerMap.put(key, value)
        notifyAll(MapChange.Put(key, oldValue, value))
        return oldValue
    }
    override fun remove(key: K): V? {
        val oldValue = innerMap.remove(key)
        oldValue?.let { notifyAll(MapChange.Remove(key, it)) }
        return oldValue
    }
    override fun clear() {
        innerMap.forEach {
            notifyAll(MapChange.Remove(it.key, it.value))
        }
        innerMap.clear()
    }
}

sealed class MapChange<T, U> {
    data class Put<T,U>(val key: T, val oldVal: U?, val newVal: U) : MapChange<T,U>()
    data class Remove<T,U>(val key: T, val oldVal: U) : MapChange<T,U>()
}