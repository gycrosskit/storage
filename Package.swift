// swift-tools-version: 5.9
import PackageDescription
let package = Package(
    name: "GycStorage", platforms: [.iOS(.v14)],
    products: [.library(name: "GycStorage", targets: ["GycStorage"])],
    dependencies: [.package(url: "https://github.com/Tencent/MMKV.git", exact: "2.4.3")],
    targets: [.target(name: "GycStorage", dependencies: [.product(name: "MMKV", package: "MMKV")], path: "ios/Sources/GycStorage")]
)
