const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { TextEncoder, TextDecoder } = require('node:util');
const ts = require(process.env.TYPESCRIPT_PATH || '/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/ets/build-tools/ets-loader/node_modules/typescript');
const entry = path.resolve(__dirname, '../ohos/storage-native');
// 只执行组件适配；MMKV 公开 API 由替身注入，真实依赖安装另行验收。

test('原生宿主保留 bytes 编码与作用域，失败向 Kotlin 返回明确结果', () => {
  const data = new Map();
  let failReads = false, failWrites = false, failRemoves = false, initialized = 0;
  class MMKV {
    static initializeWithPath(root, cache) {
      assert.equal(root, '/ability-files/mmkv');
      assert.equal(cache, '/ability-cache');
      initialized++;
    }
    static mmkvWithID(id) {
      if (!data.has(id)) data.set(id, new Map());
      const values = data.get(id);
      return {
        decodeBytes: key => failReads ? undefined : values.get(key),
        encodeBytes(key, value) { if (failWrites) return false; values.set(key, value); return true; },
        removeValuesForKeys(keys) { if (!failRemoves) keys.forEach(key => values.delete(key)); },
        containsKey: key => values.has(key),
        allKeys: () => [...values.keys()],
        count: () => BigInt(values.size),
        clearAll() { values.clear(); },
      };
    }
  }
  const source = fs.readFileSync(path.join(entry, 'src/main/ets/NativeMmkvStorage.ets'), 'utf8');
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
  const exports = {};
  vm.runInNewContext(js, { exports, require(name) {
    if (name === '@tencent/mmkv') return { MMKV };
    if (name === '@kit.ArkTS') return { util: {
      TextEncoder: class { encodeInto(value) { return new TextEncoder().encode(value); } },
      TextDecoder: class { decodeWithStream(value) { return new TextDecoder('utf-8', { fatal: true }).decode(value); } },
    } };
    throw Error(name);
  } }, { filename: 'NativeMmkvStorage.ets' });
  const storage = new exports.NativeMmkvStorage();
  const call = request => JSON.parse(storage.invoke(JSON.stringify(request)));
  assert.equal(call({ operation: 'read', storeId: 'app', key: 'value' }).ok, false);
  storage.initialize('/ability-files/mmkv', '/ability-cache'); storage.initialize('', '');
  assert.equal(initialized, 1);
  // 旧 KMP 的 encodeString 使用 encodeBytes，直接放入对应的 UTF-8 bytes 作为升级前样本。
  MMKV.mmkvWithID('app').encodeBytes('value', new TextEncoder().encode('旧数据\0😀').buffer);
  assert.deepEqual(call({ operation: 'read', storeId: 'app', key: 'value' }), { ok: true, value: '旧数据\0😀' });
  // SDK 返回 undefined 也可能是解码失败；Key 仍存在时必须阻止首次初始化清理。
  failReads = true;
  assert.deepEqual(call({ operation: 'read', storeId: 'app', key: 'value' }), { ok: false });
  assert.equal(MMKV.mmkvWithID('app').containsKey('value'), true);
  failReads = false;
  assert.equal(call({ operation: 'read', storeId: 'app', key: 'value' }).value, '旧数据\0😀');
  MMKV.mmkvWithID('app').encodeBytes('invalid_utf8', new Uint8Array([0xff]).buffer);
  assert.deepEqual(call({ operation: 'read', storeId: 'app', key: 'invalid_utf8' }), { ok: false });
  assert.equal(call({ operation: 'write', storeId: 'app', key: 'empty', value: '' }).ok, true);
  assert.deepEqual(call({ operation: 'read', storeId: 'app', key: 'empty' }), { ok: true, value: '' });
  assert.equal(call({ operation: 'write', storeId: 'user', key: 'value', value: '用户' }).ok, true);
  assert.equal(new TextDecoder().decode(data.get('user').get('value')), '用户');
  failWrites = true;
  assert.equal(call({ operation: 'write', storeId: 'user', key: 'value', value: 'replacement' }).ok, false);
  assert.equal(call({ operation: 'read', storeId: 'user', key: 'value' }).value, '用户');
  assert.equal(call({ operation: 'clear', storeId: 'user' }).ok, true);
  assert.equal(call({ operation: 'read', storeId: 'app', key: 'value' }).value, '旧数据\0😀');
  failRemoves = true;
  assert.equal(call({ operation: 'remove', storeId: 'app', keys: ['value'] }).ok, false);
  assert.equal(call({ operation: 'read', storeId: 'app', key: 'value' }).value, '旧数据\0😀');
  failRemoves = false;
  assert.equal(call({ operation: 'remove', storeId: 'app', keys: ['value'] }).ok, true);
  assert.deepEqual(call({ operation: 'read', storeId: 'app', key: 'value' }), { ok: true, value: null });
  assert.equal(call({ operation: 'clear', storeId: '../user' }).ok, false);
  failWrites = false;
  assert.equal(call({ operation: 'write', storeId: 'app', key: 'kept', value: 'retained' }).ok, true);
  assert.equal(call({ operation: 'write', storeId: 'app', key: 'removed', value: 'discard' }).ok, true);
  assert.equal(call({ operation: 'removeAllExcept', storeId: 'app', keys: ['kept'] }).ok, true);
  assert.equal(call({ operation: 'read', storeId: 'app', key: 'kept' }).value, 'retained');
  assert.equal(call({ operation: 'read', storeId: 'app', key: 'removed' }).value, null);
  assert.equal(storage.invoke('invalid'), '{"ok":false}');
});
