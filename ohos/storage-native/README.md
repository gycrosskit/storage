# @gycrosskit/storage-native

首个正式版本 `1.0.0` 待发布，提供纯 ArkTS MMKV 驱动和 C++17 桥头文件；发布与消费状态以对应渠道回执为准。

发布后按精确版本安装；当前版本尚未发布：

```sh
ohpm install @gycrosskit/storage-native@1.0.0
```

宿主以应用级实例持有 `NativeMmkvStorage`，显式注入原 `filesDir/mmkv` 与 `cacheDir`。安装函数由宿主提供，组件不依赖 Shared、Renderer 或宿主专用符号。桥头文件随包位于 `src/main/resources/rawfile/native/gyc_storage_bridge.h`。

完整合同与接线以仓库的[功能与平台差异](https://github.com/gycrosskit/storage/blob/main/docs/功能与平台差异.md)、[接入指南](https://github.com/gycrosskit/storage/blob/main/docs/接入指南.md)和[开发与验证](https://github.com/gycrosskit/storage/blob/main/docs/开发与验证.md)为准。

发布前遵循公共 [HAR 发布与消费检查点](https://github.com/gycrosskit/.github/blob/main/docs/发布流程.md#har-发布与消费检查点)。

Apache-2.0，见 [LICENSE](LICENSE)；官方 MMKV 的许可证由其依赖包提供。
