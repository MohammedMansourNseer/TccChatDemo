package dev.tcc.chat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("id") val id: Long? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("is_sent") val isSent: Boolean? = null
)
