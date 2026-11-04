#include "gyc_storage_bridge.h"
extern "C" void host_install_storage_dispatcher(GycStorageDispatcher);
napi_value InstallHostStorage(napi_env env, napi_callback_info info) {
    return GycStorageInstall(env, info, host_install_storage_dispatcher);
}
