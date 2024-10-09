package com.chatgptlite.wanted.data.remote

import android.util.Log
import com.chatgptlite.wanted.data.api.OpenAIApi
import com.chatgptlite.wanted.models.TextCompletionsParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject


class OpenAIRepositoryImpl @Inject constructor(
    private val openAIApi: OpenAIApi,
) : OpenAIRepository {
    override fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String> =
        callbackFlow {
            withContext(Dispatchers.IO) {
                val response =
                    openAIApi.textCompletionsWithOllama(params.promptText).execute()
//                    (if (params.isChatCompletions) openAIApi.textCompletionsTurboWithStream(
//                        params.toJson()
//                    ) else openAIApi.textCompletionsWithStream(params.toJson())).execute()


                if (response.isSuccessful) {
                    val input = response.body()?.byteStream()?.bufferedReader() ?: throw Exception()
                    try {
                        while (true) {
                            val line = withContext(Dispatchers.IO) {
                                input.readLine()
                            } ?: continue
                            if (line == "data: [DONE]") {
                                close()
                            } else if (line.startsWith("data:")) {
                                try {
                                    // Handle & convert data -> emit to client
                                    val value = lookupDataFromResponseTurbo(line)
//                                        if (params.isChatCompletions) lookupDataFromResponseTurbo(
//                                            line
//                                        ) else lookupDataFromResponse(
//                                            line
//                                        )

                                    if (value.isNotEmpty()) {
                                        trySend(value)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Log.e("ChatGPT Lite BUG", e.toString())
                                }
                            }
                        }
                    } catch (e: IOException) {
                        Log.e("ChatGPT Lite BUG", e.toString())
                        throw Exception(e)
                    } finally {
                        withContext(Dispatchers.IO) {
                            input.close()
                        }

                        close()
                    }
                } else {
                    if (!response.isSuccessful) {
                        var jsonObject: JSONObject? = null
                        try {
                            jsonObject = JSONObject(response.errorBody()!!.string())
                            println(jsonObject)
                            trySend("Failure! Try again. $jsonObject")
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                    trySend("Failure! Try again")
                    close()
                }
            }

            close()
        }

    override fun chatWithVoice(voice: ByteArray): Flow<String>  =
        callbackFlow {
            withContext(Dispatchers.IO) {
                val requestBody = voice.toRequestBody("audio/pcm".toMediaType())
                val audio: MultipartBody.Part = MultipartBody.Part.createFormData("file", "chat_voice", requestBody)
                //todo 将客户端的pcm编码参数加入request里
                val response = openAIApi.talkWithOllama(audio).execute()
                if (response.isSuccessful) {
                    val input = response.body()?.byteStream()?.bufferedReader() ?: throw Exception()
                    try {
                        val line = withContext(Dispatchers.IO) {
                            input.readLine()
                        }
                        trySend(line)
                    } catch (e: IOException) {
                        Log.e("ChatGPT Lite BUG", e.toString())
                        throw Exception(e)
                    } finally {
                        withContext(Dispatchers.IO) {
                            input.close()
                        }

                        close()
                    }
                } else {
                    if (!response.isSuccessful) {
                        var jsonObject: JSONObject? = null
                        try {
                            jsonObject = JSONObject(response.errorBody()!!.string())
                            println(jsonObject)
                            trySend("Failure! Try again. $jsonObject")
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                    trySend("Failure! Try again")
                    close()
                }
            }
        }


    /** Replace any double newline characters (\n\n) with a space.
    Replace any single newline characters (\n) with a space.
     */
    private fun lookupDataFromResponse(jsonString: String): String {
        val regex = """"text"\s*:\s*"([^"]+)"""".toRegex()
        val matchResult = regex.find(jsonString)

        if (matchResult != null && matchResult.groupValues.size > 1) {
            val extractedText = matchResult.groupValues[1]
            return extractedText
                .replace("\\n\\n", " ")
                .replace("\\n", " ")
        }

        return " "
    }

    private fun lookupDataFromResponseTurbo(jsonString: String): String {
        val regex = """['"]content['"]\s*:\s*['"]([^"]+)['"]""".toRegex()
        val matchResult = regex.find(jsonString)

        if (matchResult != null && matchResult.groupValues.size > 1) {
            val extractedText = matchResult.groupValues[1]
            return extractedText
                .replace("\\n\\n", " ")
                .replace("\\n", " ")
        }

        return " "
    }
}