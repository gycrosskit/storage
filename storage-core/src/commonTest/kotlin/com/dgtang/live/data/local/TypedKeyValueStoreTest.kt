package com.dgtang.live.data.local

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*

class TypedKeyValueStoreTest {
    @Serializable private data class Profile(val id: Int, val tags: List<String> = emptyList())

    @Test fun typedValuesRoundTripAndMissingOrCorruptValuesUseDefaultsWithoutRewriting() = runTest {
        val raw = NativeKeyValueStore(TestNativeStorage(), "values")
        val storage = TypedKeyValueStore(raw)
        val value = Profile(7, listOf("中文", "😀"))
        assertEquals(StorageRead.Missing, storage.read<Profile>("profile"))
        assertNull(storage.getOrNull<Profile>("profile"))
        assertTrue(storage.put("profile", value))
        assertEquals(StorageRead.Value(value), storage.read<Profile>("profile"))
        assertTrue(storage.put("enabled", false))
        assertFalse(storage.get("enabled", default = true))
        assertTrue(storage.put<String?>("nullable", null))
        assertNull(storage.get<String?>("nullable", "fallback"))
        assertTrue(storage.update<String?>("nullable", "fallback") { previous ->
            assertNull(previous)
            null
        })
        assertEquals(StorageRead.Value(null), storage.read<String?>("nullable"))
        raw.putString("profile", "{broken")
        assertEquals(StorageRead.Failure(StorageFailureReason.INVALID_DATA), storage.read<Profile>("profile"))
        assertEquals(Profile(0), storage.get("profile", Profile(0)))
        assertEquals("{broken", raw.getString("profile"))
        assertTrue(storage.update("profile", Profile(0)) { it.copy(id = 8) })
        assertEquals(Profile(8), storage.getOrNull<Profile>("profile"))
        assertTrue(storage.remove("profile"))
        assertEquals(StorageRead.Missing, storage.read<Profile>("profile"))
    }

    @Test fun ioFailuresAreResultsAndFailedUpdatesDoNotOverwriteUnreadData() = runTest {
        val native = TestNativeStorage()
        val raw = NativeKeyValueStore(native, "values")
        val storage = TypedKeyValueStore(raw) { _, _ -> error("observer failure") }
        assertTrue(storage.put("value", 7))
        native.failReads = true
        assertEquals(StorageRead.Failure(StorageFailureReason.READ_FAILED), storage.read<Int>("value"))
        assertEquals(9, storage.get("value", 9))
        assertFalse(storage.update("value", 0) { it + 1 })
        native.failReads = false
        assertEquals(7, storage.get("value", 0))
        native.failWrites = true
        assertFalse(storage.put("value", 8))
        native.failDeletes = true
        assertFalse(storage.remove("value"))
        assertEquals(7, storage.get("value", 0))
    }

    @Test fun cancellationIsNeverConvertedIntoDefaultsOrFalse() = runTest {
        val cause = CancellationException("cancelled")
        val backing = NativeKeyValueStore(TestNativeStorage(), "values")
        val raw = object : KeyValueStore by backing {
            override suspend fun getString(key: String): String = throw cause
            override suspend fun putString(key: String, value: String): Unit = throw cause
            override suspend fun updateString(key: String, transform: (String) -> String): Unit = throw cause
            override suspend fun remove(keys: Set<String>): Unit = throw cause
        }
        val storage = TypedKeyValueStore(raw)
        assertSame(cause, assertFailsWith<CancellationException> { storage.get("value", 0) })
        assertSame(cause, assertFailsWith<CancellationException> { storage.put("value", 1) })
        assertSame(cause, assertFailsWith<CancellationException> { storage.update("value", 0) { 1 } })
        assertSame(cause, assertFailsWith<CancellationException> { storage.remove("value") })
    }
}
