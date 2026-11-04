import Foundation
import MMKV
import GycStorage

@main
struct NativeStorageCompatibilityMain {
    static func main() {
        let root = ProcessInfo.processInfo.environment["GYC_STORAGE_TEST_ROOT"]
            ?? FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString).path
        let storage = GycMmkvStorage(rootDirectory: root)
        let legacy = MMKV(mmapID: "native_storage_compatibility")!
        precondition(legacy.set(Data("中文\0😀".utf8), forKey: "identity"))
        precondition(legacy.set(Data("{\"privacyAuthorization\":1,\"deviceIdBase\":\"synthetic-device\"}".utf8), forKey: "session"))
        precondition(legacy.set(Data(), forKey: "empty"))
        let identity = storage.read(storeId: "native_storage_compatibility", key: "identity")
        precondition(identity.successful && identity.value == "中文\0😀")
        let session = storage.read(storeId: "native_storage_compatibility", key: "session")
        precondition(session.successful && session.value == "{\"privacyAuthorization\":1,\"deviceIdBase\":\"synthetic-device\"}")
        let empty = storage.read(storeId: "native_storage_compatibility", key: "empty")
        precondition(empty.successful && empty.value == "")
        let absent = storage.read(storeId: "native_storage_compatibility", key: "absent")
        precondition(absent.successful && absent.value == nil)
        let native = MMKV(mmapID: "native_storage_compatibility")!
        // Key 仍存在但不能按 bytes 解码时，不能触发上层首次初始化清理。
        precondition(native.set(UInt64(65535), forKey: "invalid_bytes"))
        let invalidBytes = storage.read(storeId: "native_storage_compatibility", key: "invalid_bytes")
        precondition(!invalidBytes.successful && invalidBytes.value == nil)
        precondition(native.contains(key: "invalid_bytes"))
        precondition(native.set(Data([0xff]), forKey: "invalid_utf8"))
        precondition(!storage.read(storeId: "native_storage_compatibility", key: "invalid_utf8").successful)
        precondition(storage.write(storeId: "native_storage_compatibility", key: "replacement", value: "新值\0😀"))
        precondition(storage.write(storeId: "native_storage_other", key: "value", value: "other"))
        precondition(storage.clear(storeId: "native_storage_other"))
        precondition(storage.read(storeId: "native_storage_compatibility", key: "identity").value == "中文\0😀")
        precondition(storage.remove(storeId: "native_storage_compatibility", keys: ["identity"]))
        precondition(storage.read(storeId: "native_storage_compatibility", key: "identity").value == nil)
        precondition(storage.write(storeId: "native_storage_account", key: "user_session_json", value: "previous"))
        precondition(storage.write(storeId: "native_storage_account", key: "module.user.page:private", value: "old module data"))
        precondition(storage.removeAllExcept(storeId: "native_storage_account", keys: ["user_session_json"]))
        precondition(storage.read(storeId: "native_storage_account", key: "user_session_json").value == "previous")
        precondition(storage.read(storeId: "native_storage_account", key: "module.user.page:private").value == nil)
        print("MMKV adapter contract assertions passed")
    }
}
