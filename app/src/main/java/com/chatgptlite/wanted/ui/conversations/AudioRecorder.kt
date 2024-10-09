package com.chatgptlite.wanted.ui.conversations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioRecorder {

    //音频录制参数
    val sampleRateInHz = 16000
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
    private var audioData = ByteArray(bufferSize * 100)

    @Volatile
    var isRecording = true

    fun startRecording(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRateInHz,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                audioRecord.startRecording()
                while (isRecording) {
                    audioRecord.read(audioData, 0, audioData.size)
                }
                audioRecord.stop()
                audioRecord.release()
                audioData = ByteArray(bufferSize * 100)
            }
        }
    }

    fun stopRecording() {
        isRecording = false // 设置为false以终止协程中的录音
    }

    fun getRecordAudio(): ByteArray {
        return audioData
    }
}