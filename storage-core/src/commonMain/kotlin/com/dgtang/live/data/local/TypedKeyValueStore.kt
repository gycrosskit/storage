package com.dgtang.live.data.local

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** 只报告失败种类，异常及原始存储内容不进入结果或日志回调。 */
enum class StorageFailureReason { READ_FAILED, INVALID_DATA, WRITE_FAILED, REMOVE_FAILED }

/** 关键数据必须区分没有保存与读取失败，避免失败时按首次使用清理数据。 */
sealed interface StorageRead<out T> {
    data class Value<T>(val value: T) : StorageRead<T>
    data object Missing : StorageRead<Nothing>
    data class Failure(val reason: StorageFailureReason) : StorageRead<Nothing>
}

/** 类型编解码与失败收口；消费者提供业务 Key、默认值及可选 Json 配置。 */
class TypedKeyValueStore(
    @PublishedApi internal val values: KeyValueStore,
    @PublishedApi internal val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
    private val onFailure: (String, StorageFailureReason) -> Unit = { _, _ -> },
) {
    suspend inline fun <reified T> read(key: String): StorageRead<T> {
        val raw = try { values.getString(key) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed(key, StorageFailureReason.READ_FAILED); return StorageRead.Failure(StorageFailureReason.READ_FAILED) }
        return try { decode(key, raw, json.serializersModule.serializer<T>()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed(key, StorageFailureReason.INVALID_DATA); StorageRead.Failure(StorageFailureReason.INVALID_DATA) }
    }

    /** 缺失、读取失败或格式损坏均返回指定默认值，不覆盖原数据。 */
    suspend inline fun <reified T> get(key: String, default: T): T = when (val result = read<T>(key)) {
        is StorageRead.Value -> result.value
        else -> default
    }

    suspend inline fun <reified T> getOrNull(key: String): T? =
        (read<T>(key) as? StorageRead.Value<T>)?.value

    suspend inline fun <reified T> put(key: String, value: T): Boolean = try {
        values.putString(key, json.encodeToString(json.serializersModule.serializer<T>(), value))
        true
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { failed(key, StorageFailureReason.WRITE_FAILED); false }

    /** 读失败直接结束；只有已读到的损坏数据才使用默认值参与原子更新。 */
    suspend inline fun <reified T> update(key: String, default: T, crossinline transform: (T) -> T): Boolean = try {
        val serializer = json.serializersModule.serializer<T>()
        values.updateString(key) { raw ->
            val previous = when (val result = decode(key, raw, serializer)) {
                is StorageRead.Value -> result.value
                else -> default
            }
            json.encodeToString(serializer, transform(previous))
        }
        true
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { failed(key, StorageFailureReason.WRITE_FAILED); false }

    suspend fun remove(key: String): Boolean = try {
        values.remove(setOf(key))
        true
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { failed(key, StorageFailureReason.REMOVE_FAILED); false }

    @PublishedApi
    internal fun <T> decode(key: String, raw: String, serializer: KSerializer<T>): StorageRead<T> {
        if (raw.isEmpty()) return StorageRead.Missing
        return try { StorageRead.Value(json.decodeFromString(serializer, raw)) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed(key, StorageFailureReason.INVALID_DATA); StorageRead.Failure(StorageFailureReason.INVALID_DATA) }
    }

    @PublishedApi
    internal fun failed(key: String, reason: StorageFailureReason) {
        try { onFailure(key, reason) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* 观察者失败不能影响存储结果。 */ }
    }
}
