import Foundation
public final class MMKV {
    private static var stores: [String: [String: Any]] = [:]
    private let id: String
    public static func initialize(rootDir: String) { precondition(!rootDir.isEmpty) }
    public init?(mmapID: String) { id = mmapID }
    public func data(forKey key: String) -> Data? { Self.stores[id]?[key] as? Data }
    public func set(_ value: Data, forKey key: String) -> Bool { Self.stores[id, default: [:]][key] = value; return true }
    public func set(_ value: UInt64, forKey key: String) -> Bool { Self.stores[id, default: [:]][key] = value; return true }
    public func contains(key: String) -> Bool { Self.stores[id]?[key] != nil }
    public func removeValues(forKeys keys: [String]) { keys.forEach { Self.stores[id]?.removeValue(forKey: $0) } }
    public func allKeys() -> Any? { Array(Self.stores[id, default: [:]].keys) }
    public func clearAll() { Self.stores[id] = [:] }
    public func count() -> UInt64 { UInt64(Self.stores[id, default: [:]].count) }
}
