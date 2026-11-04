package com.dgtang.live.data.local

/** 通用跨端键值原语；业务 Key、序列化和作用域清理由消费者持有。 */
interface KeyValueStore {
    /** 读取字符串；Key 不存在时返回空字符串。 */
    suspend fun getString(key: String): String

    /** 写入完整字符串值。 */
    suspend fun putString(key: String, value: String)

    /** 串行读取、转换并写回同一个字符串 Key。 */
    suspend fun updateString(key: String, transform: (String) -> String)

    /** 删除指定 Key，不影响同一存储中的其他数据。 */
    suspend fun remove(keys: Set<String>)

    /** 保留指定 Key 并删除其余数据，保留值不经过清空与重写。 */
    suspend fun removeAllExcept(keys: Set<String>)

    /** 清空当前物理存储，清理时机由消费者决定。 */
    suspend fun clear()
}
