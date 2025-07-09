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
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant.Identity
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import io.livekit.android.rpc.RpcError

class MainActivity : AppCompatActivity() {

    lateinit var room: Room
    private lateinit var localParticipant: LocalParticipant
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Create Room object.
        room = LiveKit.create(applicationContext)

        // Setup the video renderer
        room.initVideoRenderer(findViewById<SurfaceViewRenderer>(R.id.renderer))
        room.initVideoRenderer(findViewById<TextureViewRenderer>(R.id.local_camera))

        // Setup direction control buttons
        setupDirectionButtons()

        requestNeededPermissions { connectToRoom() }
    }

    private fun connectToRoom() {
        val url = "wss://anywhere-door-uav9tfq2.livekit.cloud"
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NTgwOTA0NzcsImlzcyI6IkFQSUVqaWV5d0NXdTVYRyIsIm5hbWUiOiJwaWthYmVhciIsIm5iZiI6MTc1MjA0MjQ3Nywic3ViIjoicGlrYWJlYXIiLCJ2aWRlbyI6eyJyb29tIjoiYWxwaGEiLCJyb29tSm9pbiI6dHJ1ZX19.cQ7XAJfaj0lt4oPTZ_4CH0kIDj-nbxoAkOoxDDwruOs"
        lifecycleScope.launch {
            // Setup event handling.
            launch {
                room.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> onTrackSubscribed(event)
                        else -> {}
                    }
                }
            }

            // Connect to server.
            try {
                room.connect(
                    url,
                    token,
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error while connecting to server:", e)
                return@launch
            }

            // Turn on audio/video recording.
            localParticipant = room.localParticipant
            localParticipant.setMicrophoneEnabled(true)
            localParticipant.setCameraEnabled(true)

            // Attach local video camera
            val localTrack = localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack
            if (localTrack != null) {
                attachLocalVideo(localTrack)
            }

            // Attach video of remote participant if already available.
            val remoteVideoTrack = room.remoteParticipants.values.firstOrNull()
                ?.getTrackPublication(Track.Source.CAMERA)
                ?.track as? VideoTrack

            if (remoteVideoTrack != null) {
                attachVideo(remoteVideoTrack)
            }
        }
    }

    private fun onTrackSubscribed(event: RoomEvent.TrackSubscribed) {
        val track = event.track
        if (track is VideoTrack) {
            attachVideo(track)
        }
    }

    private fun attachVideo(videoTrack: VideoTrack) {
        videoTrack.addRenderer(findViewById<SurfaceViewRenderer>(R.id.renderer))
        findViewById<View>(R.id.progress).visibility = View.GONE
    }

    private fun attachLocalVideo(videoTrack: VideoTrack) {
        videoTrack.addRenderer(findViewById<SurfaceViewRenderer>(R.id.local_camera))
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
        findViewById<View>(R.id.btn_up).setOnClickListener { up() }
        findViewById<View>(R.id.btn_down).setOnClickListener { down() }
        findViewById<View>(R.id.btn_left).setOnClickListener { left() }
        findViewById<View>(R.id.btn_right).setOnClickListener { right() }
        findViewById<View>(R.id.btn_stop).setOnClickListener { stop() }
        findViewById<View>(R.id.btn_mute).setOnClickListener { toggleMute() }
    }

    private fun up() {
        lifecycleScope.launch {
            try {
                val response = coroutineScope {
                    async {
                        localParticipant.performRpc(
                            destinationIdentity = Identity("keith"),
                            method = "up",
                            payload = ""
                        )
                    }
                }.await()
                println("RPC response: $response")
            } catch (e: RpcError) {
                println("RPC call failed: $e")
            }
        }
        Toast.makeText(this, "向上移动", Toast.LENGTH_SHORT).show()
    }

    private fun down() {
        lifecycleScope.launch {
            try {
                val response = coroutineScope {
                    async {
                        localParticipant.performRpc(
                            destinationIdentity = Identity("keith"),
                            method = "down",
                            payload = ""
                        )
                    }
                }.await()
                println("RPC response: $response")
            } catch (e: RpcError) {
                println("RPC call failed: $e")
            }
        }
        Toast.makeText(this, "向下移动", Toast.LENGTH_SHORT).show()
    }

    private fun left() {
        lifecycleScope.launch {
            try {
                val response = coroutineScope {
                    async {
                        localParticipant.performRpc(
                            destinationIdentity = Identity("keith"),
                            method = "left",
                            payload = ""
                        )
                    }
                }.await()
                println("RPC response: $response")
            } catch (e: RpcError) {
                println("RPC call failed: $e")
            }
        }
        Toast.makeText(this, "向左移动", Toast.LENGTH_SHORT).show()
    }

    private fun right() {
        lifecycleScope.launch {
            try {
                val response = coroutineScope {
                    async {
                        localParticipant.performRpc(
                            destinationIdentity = Identity("keith"),
                            method = "right",
                            payload = ""
                        )
                    }
                }.await()
                println("RPC response: $response")
            } catch (e: RpcError) {
                println("RPC call failed: $e")
            }
        }
        Toast.makeText(this, "向右移动", Toast.LENGTH_SHORT).show()
    }

    private fun stop() {
        Toast.makeText(this, "停止移动", Toast.LENGTH_SHORT).show()
        // TODO: 实现停止移动的逻辑
    }

    private fun toggleMute() {
        if (::localParticipant.isInitialized) {
            isMuted = !isMuted
            
            lifecycleScope.launch {
                localParticipant.setMicrophoneEnabled(!isMuted)
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

    override fun onDestroy() {
        super.onDestroy()
        room.disconnect()
    }
}
