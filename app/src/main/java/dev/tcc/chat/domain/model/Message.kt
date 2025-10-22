package dev.tcc.chat.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Message(
    @SerialName("id") val id: Long = 0,
    @SerialName("content") val content: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("isSent") val isSent: Boolean
)
