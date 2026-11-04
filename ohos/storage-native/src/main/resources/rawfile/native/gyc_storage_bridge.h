#pragma once
#include "napi/native_api.h"
#include <condition_variable>
#include <cstring>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

using GycStorageDispatcher = char* (*)(const char*);
using GycStorageInstaller = void (*)(GycStorageDispatcher);

namespace gyc_storage {
inline napi_env storageEnv = nullptr;
inline napi_ref storageHandler = nullptr;
inline napi_threadsafe_function storageFunction = nullptr;
inline std::thread::id storageThread;

struct StorageCall {
    std::string input;
    std::string output = "{\"ok\":false}";
    std::mutex mutex;
    std::condition_variable completed;
    bool done = false;
};

inline std::string Invoke(napi_env env, napi_value handler, const std::string& input) {
    napi_value argument, receiver, result;
    if (napi_create_string_utf8(env, input.data(), input.size(), &argument) != napi_ok ||
        napi_get_undefined(env, &receiver) != napi_ok ||
        napi_call_function(env, receiver, handler, 1, &argument, &result) != napi_ok) {
        bool pending = false;
        napi_is_exception_pending(env, &pending);
        if (pending) napi_get_and_clear_last_exception(env, &result);
        return "{\"ok\":false}";
    }
    size_t size = 0;
    if (napi_get_value_string_utf8(env, result, nullptr, 0, &size) != napi_ok) return "{\"ok\":false}";
    std::vector<char> buffer(size + 1);
    if (napi_get_value_string_utf8(env, result, buffer.data(), buffer.size(), &size) != napi_ok) return "{\"ok\":false}";
    return std::string(buffer.data(), size);
}

inline void Complete(napi_env env, napi_value handler, void*, void* data) {
    auto* call = static_cast<StorageCall*>(data);
    std::lock_guard<std::mutex> lock(call->mutex);
    if (env && handler) call->output = Invoke(env, handler, call->input);
    call->done = true;
    call->completed.notify_one();
}

inline char* Dispatch(const char* input) {
    if (!storageFunction || !input) return nullptr;
    // 主线程直接执行；Kotlin 后台协程通过应用级 TSFN 回到 ArkTS，不持有 Renderer。
    if (std::this_thread::get_id() == storageThread) {
        napi_handle_scope scope;
        if (napi_open_handle_scope(storageEnv, &scope) != napi_ok) return nullptr;
        napi_value handler;
        std::string output = "{\"ok\":false}";
        if (napi_get_reference_value(storageEnv, storageHandler, &handler) == napi_ok) output = Invoke(storageEnv, handler, input);
        napi_close_handle_scope(storageEnv, scope);
        return strdup(output.c_str());
    }
    StorageCall call;
    call.input = input;
    std::unique_lock<std::mutex> lock(call.mutex);
    if (napi_call_threadsafe_function(storageFunction, &call, napi_tsfn_blocking) != napi_ok) return nullptr;
    call.completed.wait(lock, [&call] { return call.done; });
    return strdup(call.output.c_str());
}

inline napi_value Install(napi_env env, napi_callback_info info, GycStorageInstaller installer) {
    napi_value result;
    napi_get_undefined(env, &result);
    if (storageFunction) return result;
    size_t count = 1;
    napi_value handler, name;
    napi_get_cb_info(env, info, &count, &handler, nullptr, nullptr);
    napi_valuetype type;
    if (count != 1 || napi_typeof(env, handler, &type) != napi_ok || type != napi_function) {
        napi_throw_type_error(env, nullptr, "native storage handler is required");
        return result;
    }
    napi_create_string_utf8(env, "NativeStorage", NAPI_AUTO_LENGTH, &name);
    if (napi_create_reference(env, handler, 1, &storageHandler) != napi_ok) {
        napi_throw_error(env, nullptr, "Cannot initialize native storage bridge");
        return result;
    }
    if (napi_create_threadsafe_function(env, handler, nullptr, name, 0, 1, nullptr, nullptr, nullptr,
        Complete, &storageFunction) != napi_ok) {
        napi_delete_reference(env, storageHandler);
        storageHandler = nullptr;
        napi_throw_error(env, nullptr, "Cannot initialize native storage bridge");
        return result;
    }
    storageEnv = env;
    storageThread = std::this_thread::get_id();
    napi_unref_threadsafe_function(env, storageFunction);
    installer(Dispatch);
    return result;
}

} // namespace gyc_storage

inline napi_value GycStorageInstall(napi_env env, napi_callback_info info, GycStorageInstaller installer) {
    return gyc_storage::Install(env, info, installer);
}
