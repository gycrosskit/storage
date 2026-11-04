import { NativeMmkvStorage } from '@gycrosskit/storage-native';
export function storage(rootDirectory: string, cacheDirectory: string): NativeMmkvStorage {
  const instance = new NativeMmkvStorage();
  instance.initialize(rootDirectory, cacheDirectory);
  return instance;
}
