# TRTC SDK 迁移说明

本项目已从 LiveKit 音视频库迁移到腾讯云 TRTC SDK。

## 主要变更

### 1. 依赖库变更
- 移除：`io.livekit:livekit-android:2.18.2`
- 添加：`com.tencent.liteav:LiteAVSDK_TRTC:latest.release`

### 2. 权限配置
已在 AndroidManifest.xml 中添加 TRTC SDK 所需权限：
- INTERNET
- ACCESS_NETWORK_STATE
- ACCESS_WIFI_STATE
- RECORD_AUDIO
- MODIFY_AUDIO_SETTINGS
- BLUETOOTH
- CAMERA

### 3. 混淆配置
已在 proguard-rules.pro 中添加：
```
-keep class com.tencent.** { *; }
```

### 4. 视频渲染组件
- 替换 LiveKit 的 SurfaceViewRenderer 和 TextureViewRenderer
- 使用 TRTC 的 TXCloudVideoView

## 配置说明

### 必须配置的参数
在 `MainActivity.kt` 的 `connectToRoom()` 方法中，需要替换以下参数：

```kotlin
trtcParams.sdkAppId = 1400000123  // 替换为您的 sdkAppId
trtcParams.userId = "keith"       // 替换为您的用户ID
trtcParams.userSig = "xxx"        // 替换为您的 UserSig
trtcParams.roomId = 123321        // 替换为您的房间ID
```

### 获取参数方法
1. **sdkAppId**: 在 [腾讯云实时音视频控制台](https://console.cloud.tencent.com/trtc) 创建应用后获取
2. **UserSig**: 参考 [UserSig 生成文档](https://cloud.tencent.com/document/product/647/17275)
3. **roomId**: 自定义的房间号，建议使用数字
4. **userId**: 自定义的用户标识

## 功能对比

| 功能 | LiveKit | TRTC SDK |
|------|---------|----------|
| 进入房间 | room.connect() | mTRTCCloud.enterRoom() |
| 本地预览 | room.initVideoRenderer() | mTRTCCloud.startLocalPreview() |
| 远端视频 | 事件监听 | onUserVideoAvailable 回调 |
| 音频采集 | 自动开启 | mTRTCCloud.startLocalAudio() |
| 退出房间 | room.disconnect() | mTRTCCloud.exitRoom() |

## 注意事项

1. TRTC SDK 需要有效的 sdkAppId 和 UserSig 才能正常工作
2. UserSig 有过期时间，需要定期更新
3. 建议在正式环境中使用服务器生成 UserSig，避免在客户端暴露密钥
4. 机器人控制功能（up/down/left/right）已保留，但需要根据实际需求调整实现方式

## 参考文档

- [TRTC Android 快速集成](https://cloud.tencent.com/document/product/647/116545)
- [TRTC API 文档](https://cloud.tencent.com/document/product/647/32258)