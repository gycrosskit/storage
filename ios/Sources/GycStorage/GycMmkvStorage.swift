import Foundation
import MMKV

/** 根目录和 UTF-8 bytes 编码与原 KMP 保持一致；所有 Renderer 共用应用级实例。 */
public final class GycMmkvStorage: NSObject, GycStorageModule {
    private var stores: [String: MMKV] = [:]
    private let lock = NSLock()

    public init(rootDirectory: String) {
        super.init()
        MMKV.initialize(rootDir: rootDirectory)
    }

    private func store(_ id: String) throws -> MMKV {
        lock.lock()
        defer { lock.unlock() }
        if let cached = stores[id] { return cached }
        guard let value = MMKV(mmapID: id) else { throw failure() }
        stores[id] = value
        return value
    }

    public func read(storeId: String, key: String) -> GycStorageRead {
        do {
            let mmkv = try store(storeId)
            guard let data = mmkv.data(forKey: key) else {
                return GycStorageRead(successful: !mmkv.contains(key: key), value: nil)
            }
            guard let value = String(data: data, encoding: .utf8) else { return GycStorageRead(successful: false, value: nil) }
            return GycStorageRead(successful: true, value: value)
        } catch {
            return GycStorageRead(successful: false, value: nil)
        }
    }

    public func write(storeId: String, key: String, value: String) -> Bool {
        guard let mmkv = try? store(storeId) else { return false }
        return mmkv.set(Data(value.utf8), forKey: key)
    }

    public func remove(storeId: String, keys: [String]) -> Bool {
        guard let mmkv = try? store(storeId) else { return false }
        mmkv.removeValues(forKeys: keys)
        return keys.allSatisfy { !mmkv.contains(key: $0) }
    }

    public func removeAllExcept(storeId: String, keys: [String]) -> Bool {
        guard let mmkv = try? store(storeId), let existing = mmkv.allKeys() as? [String] else { return false }
        let retained = Set(keys)
        let removed = existing.filter { !retained.contains($0) }
        mmkv.removeValues(forKeys: removed)
        return removed.allSatisfy { !mmkv.contains(key: $0) }
    }

    public func clear(storeId: String) -> Bool {
        guard let mmkv = try? store(storeId) else { return false }
        mmkv.clearAll()
        return mmkv.count() == 0
    }

    private func failure() -> NSError {
        NSError(domain: "NativeStorageModule", code: 1, userInfo: [NSLocalizedDescriptionKey: "MMKV 存储不可用"])
    }
}
