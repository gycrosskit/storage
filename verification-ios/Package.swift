// swift-tools-version: 5.9
import PackageDescription
let package = Package(name: "StorageConsumer", platforms: [.iOS(.v14)],
    products: [.library(name: "StorageConsumer", targets: ["StorageConsumer"])],
    dependencies: [.package(url: "https://github.com/gycrosskit/storage.git", exact: "1.0.0")],
    targets: [.target(name: "StorageConsumer", dependencies: [.product(name: "GycStorage", package: "storage")])])
