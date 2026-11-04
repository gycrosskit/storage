# storage 工作规则

先阅读 README 和 docs/功能与平台差异.md。保持公共 Kotlin API、MMKV StoreID、UTF-8 bytes 与宿主注入的目录，组件不引入业务 Key、Shared 或第二套 KMP runtime。保留读取失败与缺失、取消、同实例写锁以及 removeAllExcept 不清空保留值的合同。

SDK 编译、测试、打包与消费统一由 GY CrossKit 的 `ci.py component-run` 入口执行，相关规则见 [组件开发](https://github.com/gycrosskit/.github/blob/main/docs/组件开发.md)及[统一远程验证](https://github.com/gycrosskit/.github/blob/main/docs/统一远程验证.md)。轻量原生桥/契约检查不等于真实 SDK、远程消费或设备验收。未经明确授权不提交、推送、发布或修改宿主。
