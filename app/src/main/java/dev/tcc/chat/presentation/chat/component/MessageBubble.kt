package dev.tcc.chat.presentation.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tcc.chat.domain.model.Message
import dev.tcc.chat.ui.theme.ChatTheme
import dev.tcc.chat.utililty.formatTimestamp

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSent) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    if (message.isSent) {
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        )
                    } else {
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 20.dp
                        )
                    }
                )
                .background(
                    if (message.isSent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = if (message.isSent) {
                Alignment.End
            } else {
                Alignment.Start
            }
        ) {
            Text(
                text = message.content,
                fontSize = 16.sp,
                color = if (message.isSent) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatTimestamp(message.timestamp),
                fontSize = 11.sp,
                color = if (message.isSent) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                },
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Preview(name = "Sent Message", showBackground = true)
@Composable
private fun MessageBubbleSentPreview() {
    ChatTheme {
        MessageBubble(
            message = Message(
                id = 1,
                content = "Hello! This is a sent message that demonstrates the chat bubble UI.",
                timestamp = System.currentTimeMillis(),
                isSent = true
            )
        )
    }
}

@Preview(name = "Received Message", showBackground = true)
@Composable
private fun MessageBubbleReceivedPreview() {
    ChatTheme {
        MessageBubble(
            message = Message(
                id = 2,
                content = "ok",
                timestamp = System.currentTimeMillis(),
                isSent = false
            )
        )
    }
}

@Preview(name = "Long Sent Message", showBackground = true)
@Composable
private fun MessageBubbleLongSentPreview() {
    ChatTheme {
        MessageBubble(
            message = Message(
                id = 3,
                content = "This is a much longer message that will wrap to multiple lines to demonstrate how the message bubble handles longer text content with proper formatting and spacing.",
                timestamp = System.currentTimeMillis() - 3600000,
                isSent = true
            )
        )
    }
}

@Preview(name = "Dark Mode - Sent", showBackground = true)
@Composable
private fun MessageBubbleSentDarkPreview() {
    ChatTheme(darkTheme = true) {
        MessageBubble(
            message = Message(
                id = 4,
                content = "Dark mode message",
                timestamp = System.currentTimeMillis(),
                isSent = true
            )
        )
    }
}

