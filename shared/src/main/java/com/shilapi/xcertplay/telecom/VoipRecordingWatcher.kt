package com.shilapi.xcertplay.telecom

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Reports whether another app holds a voice-over-IP call. Such calls record from VOICE_COMMUNICATION, which
 * is the only recording the car's phone audio state treats well: it nulls the echo. Plain MIC recordings,
 * the kind voice notes and video use, are muted in that state, so they never count here.
 */
internal class VoipRecordingWatcher(context: Context, private val onChange: (Boolean) -> Unit) {
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val callback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) = evaluate(configs)
    }

    fun start() {
        val manager = audio ?: return
        manager.registerAudioRecordingCallback(callback, handler)
        evaluate(manager.activeRecordingConfigurations)
    }

    fun stop() {
        audio?.unregisterAudioRecordingCallback(callback)
    }

    private fun evaluate(configs: List<AudioRecordingConfiguration>) {
        val voip = voipActive(configs.map { it.clientAudioSource }) && !PhoneAudioRoute.ownCarPlayCallActive()
        Log.i(TAG, "recording sources=${configs.map { it.clientAudioSource }} voip=$voip")
        onChange(voip)
    }

    companion object {
        private const val TAG = "DiPlay-PhoneAudio"

        fun voipActive(sources: List<Int>): Boolean = sources.any { it == MediaRecorder.AudioSource.VOICE_COMMUNICATION }
    }
}
