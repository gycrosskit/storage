import GycStorage
public func storage(rootDirectory: String) -> GycStorageModule {
    GycMmkvStorage(rootDirectory: rootDirectory)
}
