@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.dgtang.live.data.local

import kotlinx.cinterop.*
import kotlinx.serialization.json.*
import platform.posix.free

/** 进程级原生桥；ArkTS 持有官方 MMKV，后台业务调用不借用页面 Module。 */
object OhosNativeStorageModule : NativeStorageModule {
    private var dispatch: CPointer<CFunction<(CPointer<ByteVar>?) -> CPointer<ByteVar>?>>? = null

    fun install(dispatcher: CPointer<CFunction<(CPointer<ByteVar>?) -> CPointer<ByteVar>?>>) {
        check(dispatch == null) { "MMKV 原生桥只能初始化一次" }
        dispatch = dispatcher
    }

    override fun read(storeId: String, key: String): NativeStorageRead = NativeStorageRead(true,
        call("read", storeId, key)["value"]?.jsonPrimitive?.contentOrNull)

    override fun write(storeId: String, key: String, value: String): Boolean {
        call("write", storeId, key, value)
        return true
    }

    override fun remove(storeId: String, keys: List<String>): Boolean {
        call("remove", storeId, keys = keys)
        return true
    }

    override fun removeAllExcept(storeId: String, keys: List<String>): Boolean {
        call("removeAllExcept", storeId, keys = keys)
        return true
    }

    override fun clear(storeId: String): Boolean {
        call("clear", storeId)
        return true
    }

    private fun call(operation: String, storeId: String, key: String? = null, value: String? = null,
        keys: List<String>? = null): JsonObject = memScoped {
        val input = buildJsonObject {
            put("operation", operation)
            put("storeId", storeId)
            key?.let { put("key", it) }
            value?.let { put("value", it) }
            keys?.let { put("keys", JsonArray(it.map(::JsonPrimitive))) }
        }
        val raw = checkNotNull(dispatch) { "MMKV 原生桥尚未安装" }(input.toString().cstr.ptr)
            ?: error("MMKV 原生调用失败")
        val response = try { Json.parseToJsonElement(raw.toKString()).jsonObject } finally { free(raw) }
        check(response["ok"]?.jsonPrimitive?.booleanOrNull == true) { "MMKV 原生操作失败" }
        response
    }
}
