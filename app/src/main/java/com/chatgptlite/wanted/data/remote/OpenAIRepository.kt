package com.chatgptlite.wanted.data.remote

import android.media.AudioRecord
import com.chatgptlite.wanted.models.TextCompletionsParam
import kotlinx.coroutines.flow.Flow

interface OpenAIRepository {
    fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String>

    fun chatWithVoice(voice: ByteArray): Flow<String>
}