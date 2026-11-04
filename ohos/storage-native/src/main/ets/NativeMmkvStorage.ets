import { util } from '@kit.ArkTS';
import { MMKV } from '@tencent/mmkv';

interface StorageRequest {
  operation: string;
  storeId: string;
  key?: string;
  value?: string;
  keys?: string[];
}

/** 应用级实例保留原 filesDir/mmkv 和 UTF-8 bytes 格式，不随页面销毁。 */
export class NativeMmkvStorage {
  private initialized: boolean = false;
  private stores: Map<string, MMKV> = new Map();

  initialize(rootDirectory: string, cacheDirectory: string): void {
    if (this.initialized) { return; }
    MMKV.initializeWithPath(rootDirectory, cacheDirectory);
    this.initialized = true;
  }

  invoke(input: string): string {
    try {
      if (!this.initialized) { throw new Error('MMKV is not initialized'); }
      const request = JSON.parse(input) as StorageRequest;
      if (!/^[a-z][a-z0-9_]*$/.test(request.storeId)) { throw new Error('Invalid MMKV store'); }
      let mmkv = this.stores.get(request.storeId);
      if (!mmkv) {
        mmkv = MMKV.mmkvWithID(request.storeId);
        if (!mmkv) { throw new Error('Cannot open MMKV store'); }
        this.stores.set(request.storeId, mmkv);
      }
      if (request.operation === 'read' && typeof request.key === 'string') {
        const bytes = mmkv.decodeBytes(request.key);
        if (bytes === undefined && mmkv.containsKey(request.key)) { return '{"ok":false}'; }
        const value = bytes === undefined ? null : new util.TextDecoder('utf-8', { fatal: true })
          .decodeWithStream(new Uint8Array(bytes));
        return JSON.stringify({ ok: true, value: value });
      }
      if (request.operation === 'write' && typeof request.key === 'string' && typeof request.value === 'string') {
        const bytes = new util.TextEncoder().encodeInto(request.value);
        return JSON.stringify({ ok: mmkv.encodeBytes(request.key, bytes.buffer as ArrayBuffer) });
      }
      if (request.operation === 'remove' && Array.isArray(request.keys)) {
        mmkv.removeValuesForKeys(request.keys);
        for (const key of request.keys) {
          if (mmkv.containsKey(key)) { return '{"ok":false}'; }
        }
        return '{"ok":true}';
      }
      if (request.operation === 'removeAllExcept' && Array.isArray(request.keys)) {
        const retained = new Set<string>(request.keys);
        const removed = mmkv.allKeys().filter((key: string): boolean => !retained.has(key));
        mmkv.removeValuesForKeys(removed);
        for (const key of removed) {
          if (mmkv.containsKey(key)) { return '{"ok":false}'; }
        }
        return '{"ok":true}';
      }
      if (request.operation === 'clear') {
        mmkv.clearAll();
        return JSON.stringify({ ok: mmkv.count() === BigInt(0) });
      }
    } catch (_) {
      // 不把原始 Key、值或异常消息写入日志，Kotlin 所有者统一报告存储失败。
    }
    return '{"ok":false}';
  }
}
