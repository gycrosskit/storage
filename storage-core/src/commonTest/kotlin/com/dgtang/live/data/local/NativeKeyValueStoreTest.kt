package com.dgtang.live.data.local

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class NativeKeyValueStoreTest {
    @Test
    fun `concurrent updates and scoped clearing preserve the other store`() = runTest {
        val native = TestNativeStorage()
        val app = NativeKeyValueStore(native, "app")
        val user = NativeKeyValueStore(native, "user")
        app.putString("identity", "中文\u0000😀")
        user.putString("count", "0")
        val updates = List(100) { launch(Dispatchers.Default) { user.updateString("count") { ((it.toIntOrNull() ?: 0) + 1).toString() } } }
        updates.forEach { it.join() }
        assertEquals("100", user.getString("count"))
        user.clear()
        assertEquals("", user.getString("count"))
        assertEquals("中文\u0000😀", app.getString("identity"))
    }

    @Test
    fun `native failures propagate and never overwrite the previous value`() = runTest {
        val native = TestNativeStorage()
        val store = NativeKeyValueStore(native, "app")
        store.putString("value", "previous")
        native.failReads = true
        assertFailsWith<IllegalStateException> { store.getString("value") }
        native.failReads = false
        native.failWrites = true
        assertFailsWith<IllegalStateException> { store.updateString("value") { "replacement" } }
        assertEquals("previous", store.getString("value"))
        native.failDeletes = true
        assertFailsWith<IllegalStateException> { store.remove(setOf("value")) }
        assertFailsWith<IllegalStateException> { store.clear() }
        assertFailsWith<IllegalStateException> { store.removeAllExcept(setOf("value")) }
        assertEquals("previous", store.getString("value"))
        native.failWrites = false
        native.failDeletes = false
        store.remove(setOf("value"))
        assertEquals("", store.getString("value"))
    }
}

internal class TestNativeStorage : NativeStorageModule {
    val stores = mutableMapOf<String, MutableMap<String, String>>()
    var failWrites = false
    var failReads = false
    var failDeletes = false

    override fun read(storeId: String, key: String): NativeStorageRead {
        return NativeStorageRead(!failReads, if (failReads) null else stores[storeId]?.get(key))
    }

    override fun write(storeId: String, key: String, value: String): Boolean {
        if (failWrites) return false
        stores.getOrPut(storeId) { mutableMapOf() }[key] = value
        return true
    }

    override fun remove(storeId: String, keys: List<String>): Boolean {
        if (failDeletes) return false
        keys.forEach { stores[storeId]?.remove(it) }
        return true
    }

    override fun removeAllExcept(storeId: String, keys: List<String>): Boolean {
        if (failDeletes) return false
        stores[storeId]?.keys?.retainAll(keys.toSet())
        return true
    }

    override fun clear(storeId: String): Boolean {
        if (failDeletes) return false
        stores[storeId]?.clear()
        return true
    }
}
