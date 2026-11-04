const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const directory = path.resolve(__dirname, '../build/mmkv-bridge-test');
fs.mkdirSync(path.join(directory, 'napi'), { recursive: true });
fs.writeFileSync(path.join(directory, 'napi/native_api.h'), '#include <node_api.h>\n');
const source = path.resolve(__dirname, '../ohos/storage-native/src/main/resources/rawfile/native');
const output = path.join(directory, 'storage_bridge_fixture.node');
const nodeHeaders = [path.resolve(path.dirname(process.execPath), '../include/node'), '/opt/homebrew/include/node', '/usr/include/node']
  .find(directory => fs.existsSync(path.join(directory, 'node_api.h')));
assert.ok(nodeHeaders, 'NAPI 桥测试需要当前 Node 的开发头文件');
const flags = process.platform === 'darwin' ? ['-bundle', '-undefined', 'dynamic_lookup'] : ['-shared', '-fPIC', '-pthread'];
execFileSync('clang++', ['-std=c++17', ...flags, '-I' + nodeHeaders,
  '-I' + directory, '-I' + source,
  path.join(__dirname, 'native/storage_bridge_fixture.cpp'), '-o', output]);
const native = require(output);
native.install(input => {
  const request = JSON.parse(input);
  if (request.fail) throw Error('synthetic failure');
  return JSON.stringify({ ok: true, value: request.value });
});

test('应用级 NAPI 桥支持主线程和并行后台调用，不依赖 Renderer', async () => {
  const request = JSON.stringify({ value: '中文\0😀' });
  assert.deepEqual(JSON.parse(native.callMain(request)), { ok: true, value: '中文\0😀' });
  const replies = await Promise.all(Array.from({ length: 12 }, (_, index) => native.callBackground(JSON.stringify({ value: index }))));
  assert.deepEqual(replies.map(reply => JSON.parse(reply).value), Array.from({ length: 12 }, (_, index) => index));
  assert.deepEqual(JSON.parse(await native.callBackground('{"fail":true}')), { ok: false });
  assert.deepEqual(JSON.parse(native.callMain(request)), { ok: true, value: '中文\0😀' });
});
