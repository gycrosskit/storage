package com.dgtang.live.data.local

import android.content.Context
import com.tencent.mmkv.MMKV

/** 旧 KMP 字符串使用 UTF-8 bytes，继续相同编码，避免改变已有 MMKV 文件。 */
class AndroidMmkvStorage(context: Context) : NativeStorageModule {
    init { MMKV.initialize(context.applicationContext) }

    private fun store(id: String): MMKV = MMKV.mmkvWithID(id)

    override fun read(storeId: String, key: String): NativeStorageRead = store(storeId).let { mmkv ->
        val value = mmkv.decodeBytes(key)?.let { bytes ->
            try { bytes.decodeToString(throwOnInvalidSequence = true) }
            catch (_: CharacterCodingException) { mmkv.decodeString(key) }
        }
        NativeStorageRead(value != null || !mmkv.containsKey(key), value)
    }

    override fun write(storeId: String, key: String, value: String): Boolean =
        store(storeId).encode(key, value.encodeToByteArray())

    override fun remove(storeId: String, keys: List<String>): Boolean = store(storeId).let { mmkv ->
        mmkv.removeValuesForKeys(keys.toTypedArray())
        keys.none(mmkv::containsKey)
    }

    override fun removeAllExcept(storeId: String, keys: List<String>): Boolean = store(storeId).let { mmkv ->
        val existing = mmkv.allKeys() ?: return@let mmkv.count() == 0L
        val retained = keys.toSet()
        val removed = existing.filterNot(retained::contains).toTypedArray()
        mmkv.removeValuesForKeys(removed)
        removed.none(mmkv::containsKey)
    }

    override fun clear(storeId: String): Boolean = store(storeId).let { mmkv ->
        mmkv.clearAll()
        mmkv.count() == 0L
    }
}
