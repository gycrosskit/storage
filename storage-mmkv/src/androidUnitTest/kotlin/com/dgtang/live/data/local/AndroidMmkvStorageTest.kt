package com.dgtang.live.data.local

import android.content.Context
import com.tencent.mmkv.MMKV
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, shadows = [MmkvShadow::class])
class AndroidMmkvStorageTest {
    @Test fun legacyBytesAndInvalidUtf8FallbackPreserveExistingValues() {
        MmkvShadow.bytes.clear(); MmkvShadow.strings.clear()
        val storage = AndroidMmkvStorage(RuntimeEnvironment.getApplication())
        MmkvShadow.bytes["legacy"] = "中文\u0000😀".encodeToByteArray()
        assertEquals(NativeStorageRead(true, "中文\u0000😀"), storage.read("app", "legacy"))
        MmkvShadow.bytes["string"] = byteArrayOf(0xff.toByte())
        MmkvShadow.strings["string"] = "legacy MMKV string"
        assertEquals(NativeStorageRead(true, "legacy MMKV string"), storage.read("app", "string"))
        MmkvShadow.bytes["invalid"] = byteArrayOf(0xff.toByte())
        assertEquals(NativeStorageRead(false, null), storage.read("app", "invalid"))
        assertEquals(NativeStorageRead(true, null), storage.read("app", "missing"))
        assertTrue(storage.write("app", "kept", "retained"))
        assertTrue(storage.removeAllExcept("app", listOf("kept")))
        assertEquals(NativeStorageRead(true, "retained"), storage.read("app", "kept"))
        assertFalse(MmkvShadow.bytes.containsKey("legacy"))
    }
}

@Implements(value = MMKV::class, isInAndroidSdk = false)
class MmkvShadow {
    companion object {
        val bytes = mutableMapOf<String, ByteArray>()
        val strings = mutableMapOf<String, String>()
        @Implementation @JvmStatic fun initialize(context: Context): String = context.filesDir.path
        @Implementation @JvmStatic fun mmkvWithID(id: String): MMKV = MMKV::class.java.getDeclaredConstructor(java.lang.Long.TYPE).apply { isAccessible = true }.newInstance(0L)
    }
    @Implementation fun decodeBytes(key: String): ByteArray? = bytes[key]
    @Implementation fun decodeString(key: String): String? = strings[key]
    @Implementation fun containsKey(key: String): Boolean = key in bytes || key in strings
    @Implementation fun encode(key: String, value: ByteArray): Boolean { bytes[key] = value; return true }
    @Implementation fun allKeys(): Array<String> = (bytes.keys + strings.keys).toTypedArray()
    @Implementation fun removeValuesForKeys(keys: Array<String>) { keys.forEach { bytes.remove(it); strings.remove(it) } }
    @Implementation fun clearAll() { bytes.clear(); strings.clear() }
    @Implementation fun count(): Long = (bytes.keys + strings.keys).size.toLong()
}
