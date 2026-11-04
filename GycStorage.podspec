Pod::Spec.new do |spec|
  spec.name = 'GycStorage'
  spec.version = '0.1.0-rc.1'
  spec.summary = '保留 MMKV UTF-8 bytes 格式的原生键值存储'
  spec.homepage = 'https://github.com/gycrosskit/storage'
  spec.license = { :type => 'Apache-2.0', :file => 'LICENSE' }
  spec.author = { 'GY CrossKit' => 'https://github.com/gycrosskit' }
  spec.source = { :git => 'https://github.com/gycrosskit/storage.git', :tag => spec.version.to_s }
  spec.ios.deployment_target = '14.0'
  spec.swift_version = '5.9'
  spec.source_files = 'ios/Sources/GycStorage/**/*.swift'
  spec.dependency 'MMKV', '2.4.3'
end
