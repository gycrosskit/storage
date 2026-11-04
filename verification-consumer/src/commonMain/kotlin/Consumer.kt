import com.dgtang.live.data.local.*

suspend fun storageContract(native: NativeStorageModule) {
    val values: KeyValueStore = NativeKeyValueStore(native, "consumer")
    val typed = TypedKeyValueStore(values)
    check(typed.put("value", "中文\u0000😀"))
    check(typed.get("value", "") == "中文\u0000😀")
    values.removeAllExcept(setOf("value"))
}
