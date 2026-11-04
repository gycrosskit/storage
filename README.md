# GY CrossKit Storage

通用键值存储组件，迁自 sxmqliveAndroid `c768cb12c` 的 storage-core、storage-mmkv 及原生 MMKV 驱动。业务 Key、账号、Json 默认值、清理时机、目录和应用级实例由宿主持有。当前 `0.1.0-rc.1` 为源码候选，公开渠道状态以实际发布回执为准。

| 产品 | 职责 | 平台与入口 |
| --- | --- | --- |
| storage-core | 字符串原语、类型编解码、读写失败、同实例读改写锁 | Android/iOS/OHOS/JVM；`KeyValueStore`、`TypedKeyValueStore`、`NativeKeyValueStore` |
| storage-mmkv | 官方 MMKV 的 Android 驱动 | Android；`AndroidMmkvStorage` |
| GycStorage | 纯 Swift MMKV 驱动，无 Shared/KMP runtime | iOS；`GycMmkvStorage`、`GycStorageModule`、`GycStorageRead` |
| @gycrosskit/storage-native | 纯 ArkTS MMKV 驱动与可复用 C++17 TSFN 头文件 | OHOS；`NativeMmkvStorage`、`GycStorageInstall` |

没有 UI/Renderer 产品。KMP iOS 通过宿主薄 adapter 使用 Swift 驱动；OHOS Kotlin 边界为 `OhosNativeStorageModule`。Kotlin 包暂保留 `com.dgtang.live.data.local`，避免迁移同时改变调用方 API。

```kotlin
implementation("com.github.gycrosskit.storage:storage-core:0.1.0-rc.1")
// Android source set
implementation("com.github.gycrosskit.storage:storage-mmkv:0.1.0-rc.1")
```

Maven 仓库为 `https://jitpack.io`，仅精确版本依赖。iOS CocoaPods：`pod 'GycStorage', :git => 'https://github.com/gycrosskit/storage.git', :tag => 'native-0.1.0-rc.1'`（未发布到 Specs）；SPM：本仓库精确版本、产品 `GycStorage`。OHPM：`@gycrosskit/storage-native` 精确版本 `0.1.0-rc.1`。声明坐标不代表渠道已就绪。

```kotlin
val native = AndroidMmkvStorage(context)
// 一个物理 StoreID 只创建一个实例，读改写由该实例串行化。
val values = NativeKeyValueStore(native, "app")
val typed = TypedKeyValueStore(values)
```

MMKV 固定 2.4.3。iOS 最低 14；Android 最低 24、compileSdk 36；OHOS 工具链沿用 API22/6.0.2。SPM 的上游 MMKV manifest 需 Swift 6.2，组件没有升级宿主工具链。

- [接入指南](docs/接入指南.md)：目录、实例、Swift adapter 与 OHOS NAPI 接线。
- [功能与平台差异](docs/功能与平台差异.md)：唯一功能合同及兼容范围。
- [开发与验证](docs/开发与验证.md)：轻量回归、SDK/消费目标与发布输入。

采用 Apache-2.0；官方 MMKV 由各包管理器解析，其许可证由上游维护。
