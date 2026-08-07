package com.accessnow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.webrtc.*
import java.util.concurrent.Executors

class WebRTCClient : AppCompatActivity() {
    private val TAG = "WebRTCClient"
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var peerConnection: PeerConnection
    private var dataChannel: DataChannel? = null
    private lateinit var eglBase: EglBase
    private lateinit var videoView: SurfaceViewRenderer
    private lateinit var webSocket: WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webrtc)
        videoView = findViewById(R.id.remote_view)
        initPeerConnectionFactory()
        initDataChannel()
        connectToSignalServer()
    }

    private fun initPeerConnectionFactory() {
        EglBase.create().let { egl ->
            EGLSurface = egl
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options()
                    .setDisableWebAudio(true))
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl, true, true))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl))
                .createPeerConnectionFactory()
        }
    }

    private fun initDataChannel() {
        val config = DataChannel.Init().apply {
            ordered = true
            maxRetransmits = 0
        }
        dataChannel = peerConnection.createDataChannel("coords", config)
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {}
            override fun onMessage(msg: DataChannel.Message) {
                val text = String(msg.data?.array() ?: byteArrayOf())
                Log.i(TAG, "Received coords: $text")
            }
        })
    }

    private fun connectToSignalServer() {
        val wsUrl = "wss://YOUR-SERVER-DOMAIN:3000"
        webSocket = OkHttpClient().newWebSocket(
            Request.Builder().url(wsUrl).build(),
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Handle SDP/ICE messages
                    runOnUiThread { processSignalMessage(text) }
                }
            })
    }

    private fun processSignalMessage(message: String) {
        // Simple JSON routing: {type, payload}
        val obj = JSONObject(message)
        when (obj.getString("type")) {
            "offer" -> onOffer(obj.getJSONObject("payload"))
            "answer" -> onAnswer(obj.getJSONObject("payload"))
            "candidate" -> onIceCandidate(obj.getJSONObject("payload"))
        }
    }

    private fun onOffer(sdp: JSONObject) {
        val sdpObj = SessionDescription(SessionDescription.Type.OFFER, sdp.getString("sdp"))
        peerConnection.setRemoteDescription(object : SdpObserver { /* implement */ }, sdpObj)
        // Create answer
        peerConnection.createAnswer(object : SdpObserver { /* implement send */ }, MediaConstraints())
    }

    private fun onAnswer(sdp: JSONObject) {
        val sdpObj = SessionDescription(SessionDescription.Type.ANSWER, sdp.getString("sdp"))
        peerConnection.setRemoteDescription(object : SdpObserver { /* implement */ }, sdpObj)
    }

    private fun onIceCandidate(json: JSONObject) {
        val candidate = IceCandidate(
            json.getString("sdpMid"),
            json.getInt("sdpMLineIndex"),
            json.getString("candidate")
        )
        peerConnection.addIceCandidate(candidate)
    }

    override fun onDestroy() {
        super.onDestroy()
        peerConnection.close()
        videoView.release()
    }
}
