// 仅用于升级回归：与原锁定 KMP 制品中的 libmmkv-kmp.a、同 Tag 的 MMKVBridge.h 一起链接。
#import <Foundation/Foundation.h>
#include "MMKVBridge.h"
#include <cassert>
#include <cstring>
#include <cstdlib>
#include <string>

int main(int argc, char** argv) {
    @autoreleasepool {
        const char* directory = std::getenv("GYC_STORAGE_TEST_ROOT");
        assert(directory && directory[0]);
        NSString* root = [NSString stringWithUTF8String:directory];
        assert([[NSFileManager defaultManager] createDirectoryAtPath:root withIntermediateDirectories:YES attributes:nil error:nil]);
        mmkv_initialize(root.UTF8String, 4);
        MMKVConfig_t config = {};
        config.mode = 1;
        config.enableKeyExpire = -1;
        config.recover = -1;
        auto store = mmkv_with_id("native_storage_compatibility", config);
        assert(store);
        if (argc > 1 && std::strcmp(argv[1], "verify") == 0) {
            const std::string expected = std::string("新值\0", 7) + "😀";
            uint64_t length = 0;
            void* bytes = mmkv_decode_bytes(store, "replacement", &length);
            assert(bytes && std::string(static_cast<char*>(bytes), length) == expected);
            mmkv_free(bytes);
        } else {
            const std::string sample = std::string("中文\0", 7) + "😀";
            assert(mmkv_encode_bytes(store, "identity", sample.data(), sample.size()));
            const std::string json = "{\"privacyAuthorization\":1,\"deviceIdBase\":\"synthetic-device\"}";
            assert(mmkv_encode_bytes(store, "session", json.data(), json.size()));
            assert(mmkv_encode_bytes(store, "empty", "", 0));
        }
        mmkv_close(store);
        puts("legacy MMKV compatibility check passed");
    }
}
