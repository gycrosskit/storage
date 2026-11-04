#include <node_api.h>
#include "gyc_storage_bridge.h"
#include <cstdlib>
#include <string>

static char* (*dispatchStorage)(const char*) = nullptr;
static void InstallDispatcher(char* (*dispatch)(const char*)) { dispatchStorage = dispatch; }

static std::string Read(napi_env env, napi_value value) {
    size_t size;
    napi_get_value_string_utf8(env, value, nullptr, 0, &size);
    std::string text(size, '\0');
    napi_get_value_string_utf8(env, value, text.data(), size + 1, &size);
    return text;
}

static std::string Dispatch(const std::string& text) {
    char* raw = dispatchStorage(text.c_str());
    std::string result = raw ? raw : "";
    std::free(raw);
    return result;
}

static napi_value CallMain(napi_env env, napi_callback_info info) {
    size_t count = 1;
    napi_value argument, result;
    napi_get_cb_info(env, info, &count, &argument, nullptr, nullptr);
    const auto response = Dispatch(Read(env, argument));
    napi_create_string_utf8(env, response.c_str(), response.size(), &result);
    return result;
}

struct Work { napi_async_work work; napi_deferred deferred; std::string input; std::string output; };
static napi_value CallBackground(napi_env env, napi_callback_info info) {
    size_t count = 1;
    napi_value argument, result, name;
    napi_get_cb_info(env, info, &count, &argument, nullptr, nullptr);
    auto* work = new Work();
    work->input = Read(env, argument);
    napi_create_promise(env, &work->deferred, &result);
    napi_create_string_utf8(env, "StorageWorker", NAPI_AUTO_LENGTH, &name);
    napi_create_async_work(env, nullptr, name, [](napi_env, void* data) {
        auto* work = static_cast<Work*>(data); work->output = Dispatch(work->input);
    }, [](napi_env env, napi_status, void* data) {
        auto* work = static_cast<Work*>(data);
        napi_value value;
        napi_create_string_utf8(env, work->output.c_str(), work->output.size(), &value);
        napi_resolve_deferred(env, work->deferred, value);
        napi_delete_async_work(env, work->work);
        delete work;
    }, work, &work->work);
    napi_queue_async_work(env, work->work);
    return result;
}

static napi_value Install(napi_env env, napi_callback_info info) { return GycStorageInstall(env, info, InstallDispatcher); }

static napi_value Init(napi_env env, napi_value exports) {
    napi_property_descriptor properties[] = {
        {"install", nullptr, Install, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"callMain", nullptr, CallMain, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"callBackground", nullptr, CallBackground, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, 3, properties);
    return exports;
}
NAPI_MODULE(storage_bridge_fixture, Init)
