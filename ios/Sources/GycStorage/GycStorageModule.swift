import Foundation

/** 原生读取保留缺失与失败，宿主将其映射到自己的跨语言类型。 */
public struct GycStorageRead {
    public let successful: Bool
    public let value: String?
    public init(successful: Bool, value: String?) { self.successful = successful; self.value = value }
}

public protocol GycStorageModule {
    func read(storeId: String, key: String) -> GycStorageRead
    func write(storeId: String, key: String, value: String) -> Bool
    func remove(storeId: String, keys: [String]) -> Bool
    func removeAllExcept(storeId: String, keys: [String]) -> Bool
    func clear(storeId: String) -> Bool
}
