package com.dgtang.live.data.local

/** 原生 Storage Module 的读取结果，区分不存在和存储失败，兼容 Swift/ArkTS 同步桥。 */
data class NativeStorageRead(val successful: Boolean, val value: String?)

/** 应用级原生存储边界；实现层可替换，不依赖 Renderer 生命周期或具体存储 SDK。 */
interface NativeStorageModule {
    fun read(storeId: String, key: String): NativeStorageRead
    fun write(storeId: String, key: String, value: String): Boolean
    fun remove(storeId: String, keys: List<String>): Boolean
    /** 保留指定 Key 并删除其余数据，不能先清空再写回保留值。 */
    fun removeAllExcept(storeId: String, keys: List<String>): Boolean
    fun clear(storeId: String): Boolean
}

/** 只有 Key 不存在才返回 null；读取失败必须向调用方传播。 */
fun NativeStorageModule.readString(storeId: String, key: String): String? {
    val result = read(storeId, key)
    check(result.successful) { "原生存储读取失败" }
    return result.value
}
