# TRTC 自定义消息实现指南

本文档介绍如何使用腾讯云 TRTC SDK 的自定义消息功能，实现从 app-android 模块到 app 模块的方向控制指令发送。

## 功能概述

通过 TRTC 自定义消息功能，app-android 模块可以向 app 模块发送以下控制指令：
- 向上移动（UP）
- 向下移动（DOWN）
- 向左移动（LEFT）
- 向右移动（RIGHT）
- 停止移动（STOP）

## 实现架构

```
app-android 模块          TRTC 房间          app 模块
    ↓                        ↓                ↓
用户点击按钮  →  发送自定义消息  →  接收消息并控制机器人
```

## 消息协议定义

### 命令ID定义
- `CMD_ID_DIRECTION = 1`: 方向控制命令
- `CMD_ID_STOP = 2`: 停止命令

### 消息内容
- `"UP"`: 向前移动
- `"DOWN"`: 向后移动
- `"LEFT"`: 向左转
- `"RIGHT"`: 向右转
- `"STOP"`: 停止移动

## app-android 模块实现

### 1. 发送自定义消息

```kotlin
/**
 * 发送自定义消息
 * @param cmdId 命令ID
 * @param message 消息内容
 */
private fun sendCustomMessage(cmdId: Int, message: String) {
    if (::mTRTCCloud.isInitialized) {
        try {
            val data = message.toByteArray(Charsets.UTF_8)
            mTRTCCloud.sendCustomCmdMsg(cmdId, data, true, true)
            Log.d("MainActivity", "Sent custom message: cmdId=$cmdId, message=$message")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to send custom message", e)
            Toast.makeText(this, "发送消息失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(this, "请先连接到房间", Toast.LENGTH_SHORT).show()
    }
}
```

### 2. 按钮点击事件处理

```kotlin
private fun up() {
    sendCustomMessage(CMD_ID_DIRECTION, DIRECTION_UP)
    Toast.makeText(this, "发送向上移动指令", Toast.LENGTH_SHORT).show()
}

private fun down() {
    sendCustomMessage(CMD_ID_DIRECTION, DIRECTION_DOWN)
    Toast.makeText(this, "发送向下移动指令", Toast.LENGTH_SHORT).show()
}

private fun left() {
    sendCustomMessage(CMD_ID_DIRECTION, DIRECTION_LEFT)
    Toast.makeText(this, "发送向左移动指令", Toast.LENGTH_SHORT).show()
}

private fun right() {
    sendCustomMessage(CMD_ID_DIRECTION, DIRECTION_RIGHT)
    Toast.makeText(this, "发送向右移动指令", Toast.LENGTH_SHORT).show()
}

private fun stop() {
    sendCustomMessage(CMD_ID_STOP, DIRECTION_STOP)
    Toast.makeText(this, "发送停止移动指令", Toast.LENGTH_SHORT).show()
}
```

## app 模块实现

### 1. 接收自定义消息

```kotlin
override fun onRecvCustomCmdMsg(userId: String?, cmdId: Int, seq: Int, message: ByteArray?) {
    super.onRecvCustomCmdMsg(userId, cmdId, seq, message)
    Log.d(TAG, "收到来自 $userId 的自定义消息, cmdId: $cmdId")
    
    message?.let {
        val messageStr = String(it, Charsets.UTF_8)
        Log.d(TAG, "消息内容: $messageStr")
        
        when (cmdId) {
            CMD_ID_DIRECTION -> {
                handleDirectionCommand(messageStr)
            }
            CMD_ID_STOP -> {
                handleStopCommand()
            }
            else -> {
                Log.w(TAG, "未知的命令ID: $cmdId")
            }
        }
    }
}
```

### 2. 处理方向控制命令

```kotlin
/**
 * 处理方向控制命令
 * @param direction 方向指令
 */
private fun handleDirectionCommand(direction: String) {
    runOnUiThread {
        when (direction) {
            DIRECTION_UP -> {
                showToast("执行向前移动")
                Log.d(TAG, "执行向前移动指令")
                // 使用Nuwa机器人SDK控制机器人向前移动
                mRobot.motionPlay("forward", false)
            }
            DIRECTION_DOWN -> {
                showToast("执行向后移动")
                Log.d(TAG, "执行向后移动指令")
                // 使用Nuwa机器人SDK控制机器人向后移动
                mRobot.motionPlay("backward", false)
            }
            DIRECTION_LEFT -> {
                showToast("执行向左转")
                Log.d(TAG, "执行向左转指令")
                // 使用Nuwa机器人SDK控制机器人向左转
                mRobot.motionPlay("turn_left", false)
            }
            DIRECTION_RIGHT -> {
                showToast("执行向右转")
                Log.d(TAG, "执行向右转指令")
                // 使用Nuwa机器人SDK控制机器人向右转
                mRobot.motionPlay("turn_right", false)
            }
            else -> {
                Log.w(TAG, "未知的方向指令: $direction")
                showToast("未知的方向指令: $direction")
            }
        }
    }
}
```

### 3. 处理停止命令

```kotlin
/**
 * 处理停止命令
 */
private fun handleStopCommand() {
    runOnUiThread {
        showToast("执行停止移动")
        Log.d(TAG, "执行停止移动指令")
        // 使用Nuwa机器人SDK停止机器人运动
        mRobot.motionStop()
        // 重置机器人姿态
        mRobot.motionReset()
    }
}
```

## 使用限制

根据腾讯云 TRTC 文档，自定义消息有以下限制：

1. **频率限制**: 每秒最多能发送 30 条消息
2. **消息大小**: 每个消息包最大为 1KB
3. **总数据量**: 每个客户端每秒最多能发送总计 8KB 数据
4. **权限要求**: 仅主播身份可以发送自定义消息

## 注意事项

1. **房间连接**: 确保两个模块都成功连接到同一个 TRTC 房间
2. **用户角色**: app-android 模块需要以主播身份进房才能发送自定义消息
3. **错误处理**: 实现适当的错误处理和重试机制
4. **消息可靠性**: 使用 `reliable=true` 和 `ordered=true` 确保消息的可靠传输
5. **线程安全**: UI 更新操作需要在主线程中执行

## 测试步骤

1. 启动 app 模块，确保机器人服务正常运行
2. 启动 app-android 模块，连接到同一个 TRTC 房间
3. 在 app-android 模块中点击方向控制按钮
4. 观察 app 模块是否收到消息并执行相应的机器人动作
5. 检查日志输出确认消息发送和接收状态

## 扩展功能

可以基于此实现扩展更多功能：
- 添加更多机器人控制指令（如头部转动、手臂动作等）
- 实现双向通信（机器人状态反馈）
- 添加消息确认机制
- 实现批量指令发送
- 添加指令队列和优先级处理