package com.dgtang.live.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 同一 Store 只装配一个实例，所有写操作共用协程锁，防止读改写覆盖。
 * 原生 MMKV 由应用持有，页面释放不能关闭其他调用方仍在使用的实例。
 */
class NativeKeyValueStore(
    private val storage: NativeStorageModule,
    private val storeId: String,
) : KeyValueStore {
    private val writeMutex = Mutex()
    override suspend fun getString(key: String): String = storage.readString(storeId, key).orEmpty()

    override suspend fun putString(key: String, value: String) {
        writeMutex.withLock { write(key, value) }
    }

    override suspend fun updateString(
        key: String,
        transform: (String) -> String,
    ) {
        writeMutex.withLock {
            write(key, transform(storage.readString(storeId, key).orEmpty()))
        }
    }

    override suspend fun remove(keys: Set<String>) {
        if (keys.isEmpty()) return
        writeMutex.withLock { check(storage.remove(storeId, keys.toList())) { "原生存储删除失败" } }
    }

    override suspend fun removeAllExcept(keys: Set<String>) {
        writeMutex.withLock { check(storage.removeAllExcept(storeId, keys.toList())) { "原生存储清理失败" } }
    }

    override suspend fun clear() {
        writeMutex.withLock { check(storage.clear(storeId)) { "原生存储清空失败" } }
    }

    private fun write(key: String, value: String) {
        check(storage.write(storeId, key, value)) { "原生存储写入失败：$key" }
    }
}
