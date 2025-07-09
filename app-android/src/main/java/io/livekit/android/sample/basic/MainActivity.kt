/*
 * Copyright 2024 LiveKit, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.livekit.android.sample.basic

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tencent.trtc.TRTCCloud
import com.tencent.trtc.TRTCCloudDef
import com.tencent.trtc.TRTCCloudListener
import com.tencent.rtmp.ui.TXCloudVideoView
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async

class MainActivity : AppCompatActivity() {

    private lateinit var mTRTCCloud: TRTCCloud
    private var isMuted = false
    
    // 自定义消息命令ID定义
    companion object {
        private const val CMD_ID_DIRECTION = 1  // 方向控制命令
        private const val CMD_ID_STOP = 2       // 停止命令
        private const val CMD_ID_NECK = 3       // 脖子控制命令
        
        // 方向命令
        private const val DIRECTION_UP = "UP"
        private const val DIRECTION_DOWN = "DOWN"
        private const val DIRECTION_LEFT = "LEFT"
        private const val DIRECTION_RIGHT = "RIGHT"
        private const val DIRECTION_STOP = "STOP"
        
        // 脖子控制命令
        private const val NECK_UP = "NECK_UP"
        private const val NECK_DOWN = "NECK_DOWN"
        private const val NECK_LEFT = "NECK_LEFT"
        private const val NECK_RIGHT = "NECK_RIGHT"
        private const val NECK_RESET = "NECK_RESET"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Initialize TRTC SDK
        mTRTCCloud = TRTCCloud.sharedInstance(applicationContext)
        mTRTCCloud.setListener(mTRTCCloudListener)

        // Setup direction control buttons
        setupDirectionButtons()

        requestNeededPermissions { connectToRoom() }
    }

    private val mTRTCCloudListener = object : TRTCCloudListener() {
        override fun onError(errCode: Int, errMsg: String?, extraInfo: Bundle?) {
            Log.e("MainActivity", "TRTC Error: $errCode, $errMsg")
        }

        override fun onEnterRoom(result: Long) {
            if (result > 0) {
                Log.d("MainActivity", "Enter room success")
                startLocalPreview()
            } else {
                Log.e("MainActivity", "Enter room failed: $result")
            }
        }

        override fun onExitRoom(reason: Int) {
            Log.d("MainActivity", "Exit room: $reason")
        }

        override fun onRemoteUserEnterRoom(userId: String?) {
            Log.d("MainActivity", "Remote user enter: $userId")
        }

        override fun onRemoteUserLeaveRoom(userId: String?, reason: Int) {
            Log.d("MainActivity", "Remote user leave: $userId")
        }

        override fun onUserVideoAvailable(userId: String?, available: Boolean) {
            if (available) {
                val remoteView = findViewById<TXCloudVideoView>(R.id.renderer)
                mTRTCCloud.startRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, remoteView)
                findViewById<View>(R.id.progress).visibility = View.GONE
            } else {
                mTRTCCloud.stopRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG)
            }
        }

        override fun onUserAudioAvailable(userId: String?, available: Boolean) {
            Log.d("MainActivity", "User audio available: $userId, $available")
        }
        
    }

    private fun connectToRoom() {
        // TRTC 参数配置
        val params = TRTCCloudDef.TRTCParams()
        params.sdkAppId = 1600096140 // 请替换为您的SDKAppID
        params.userId = "pikabear" // 用户ID
        params.userSig = "eJwtzE0LgkAUheH-MttCrqbjKLgoahG4CDRoO3Jn5GbKNJoZ0X-Pr*V5XjhflqeZ0yvLYuY5wLbzJlRNR5pmNlTJQkm7thYraQwhi10OABF3fViKGgxZNXoQBN6YFu2oniwcifsictcXKqfrR5pnsj3uDqJ4*7UenvcNNq9yf76d8DLYQuAHQo3QXxP2*wP79TNG" // 请替换为您的UserSig
        params.roomId = 12345 // 房间号

        // 进入房间
        mTRTCCloud.enterRoom(params, TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL)
    }

    private fun startLocalPreview() {
        val localView = findViewById<TXCloudVideoView>(R.id.local_camera)
        mTRTCCloud.startLocalPreview(true, localView)
        mTRTCCloud.startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_DEFAULT)
    }
    
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
    
   

    private fun requestNeededPermissions(onHasPermissions: () -> Unit) {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
                var hasDenied = false
                // Check if any permissions weren't granted.
                for (grant in grants.entries) {
                    if (!grant.value) {
                        Toast.makeText(this, "Missing permission: ${grant.key}", Toast.LENGTH_SHORT).show()

                        hasDenied = true
                    }
                }

                if (!hasDenied) {
                    onHasPermissions()
                }
            }

        // Assemble the needed permissions to request
        val neededPermissions = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .filter { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED }
            .toTypedArray()

        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions)
        } else {
            onHasPermissions()
        }
    }

    private fun setupDirectionButtons() {
        // 设置方向按钮的触摸监听器，按下时执行动作，释放时停止
        findViewById<View>(R.id.btn_up).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    up()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stop()
                    true
                }
                else -> false
            }
        }
        
        findViewById<View>(R.id.btn_down).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    down()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stop()
                    true
                }
                else -> false
            }
        }
        
        findViewById<View>(R.id.btn_left).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    left()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stop()
                    true
                }
                else -> false
            }
        }
        
        findViewById<View>(R.id.btn_right).setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    right()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stop()
                    true
                }
                else -> false
            }
        }
        
        // 停止按钮和静音按钮保持原有的点击监听器
        findViewById<View>(R.id.btn_stop).setOnClickListener { stop() }
        findViewById<View>(R.id.btn_mute).setOnClickListener { toggleMute() }
        
        // 设置脖子控制按钮的触摸监听器
        findViewById<View>(R.id.btn_neck_up).setOnClickListener { neckUp() }
        
        findViewById<View>(R.id.btn_neck_down).setOnClickListener { neckDown() }
        findViewById<View>(R.id.btn_neck_left).setOnClickListener { neckLeft() }
        findViewById<View>(R.id.btn_neck_right).setOnClickListener { neckRight() }
       
        // 脖子复位按钮使用点击监听器
        findViewById<View>(R.id.btn_neck_reset).setOnClickListener { neckReset() }
    }

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
    
    // 脖子控制方法
    private fun neckUp() {
        sendCustomMessage(CMD_ID_NECK, NECK_UP)
        Toast.makeText(this, "发送抬头指令", Toast.LENGTH_SHORT).show()
    }
    
    private fun neckDown() {
        sendCustomMessage(CMD_ID_NECK, NECK_DOWN)
        Toast.makeText(this, "发送低头指令", Toast.LENGTH_SHORT).show()
    }
    
    private fun neckLeft() {
        sendCustomMessage(CMD_ID_NECK, NECK_LEFT)
        Toast.makeText(this, "发送脖子左转指令", Toast.LENGTH_SHORT).show()
    }
    
    private fun neckRight() {
        sendCustomMessage(CMD_ID_NECK, NECK_RIGHT)
        Toast.makeText(this, "发送脖子右转指令", Toast.LENGTH_SHORT).show()
    }
    
    private fun neckReset() {
        sendCustomMessage(CMD_ID_NECK, NECK_RESET)
        Toast.makeText(this, "发送脖子复位指令", Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleMute() {
        if (::mTRTCCloud.isInitialized) {
            isMuted = !isMuted
            
            if (isMuted) {
                mTRTCCloud.muteLocalAudio(true)
            } else {
                mTRTCCloud.muteLocalAudio(false)
            }
            
            val muteButton = findViewById<android.widget.Button>(R.id.btn_mute)
            muteButton.text = if (isMuted) "取消静音" else "静音"
            
            val message = if (isMuted) "麦克风已静音" else "麦克风已开启"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "Microphone muted: $isMuted")
        } else {
            Toast.makeText(this, "请先连接到房间", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // 处理屏幕方向变化
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Log.d("MainActivity", "切换到横屏模式")
                Toast.makeText(this, "横屏模式", Toast.LENGTH_SHORT).show()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                Log.d("MainActivity", "切换到竖屏模式")
                Toast.makeText(this, "竖屏模式", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 重新设置按钮监听器，确保在布局变化后按钮功能正常
        setupDirectionButtons()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mTRTCCloud.isInitialized) {
            mTRTCCloud.stopLocalPreview()
            mTRTCCloud.stopLocalAudio()
            mTRTCCloud.exitRoom()
            mTRTCCloud.setListener(null)
            TRTCCloud.destroySharedInstance()
        }
    }
}
