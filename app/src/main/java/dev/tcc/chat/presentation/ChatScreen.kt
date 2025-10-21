package dev.tcc.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tcc.chat.domain.model.Message
import dev.tcc.chat.presentation.chat.component.MessageBubble
import dev.tcc.chat.presentation.chat.contract.ChatIntent
import dev.tcc.chat.ui.theme.ChatTheme
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Encrypted Chat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.messageCount} messages",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(
                        onClick = { viewModel.handleIntent(ChatIntent.InsertLargeDataset(1000)) }
                    ) {
                        Text("+ 1K")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = state.inputText,
                onInputChange = { viewModel.handleIntent(ChatIntent.UpdateInputText(it)) },
                onSendClick = {
                    viewModel.handleIntent(ChatIntent.SendMessage(state.inputText))
                },
                isSending = state.isSending,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.messages.isEmpty()) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(message = message)
                    }
                }
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                enabled = !isSending,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                maxLines = 4
            )

            FilledIconButton(
                onClick = onSendClick,
                enabled = inputText.isNotBlank() && !isSending,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message"
                    )
                }
            }
        }
    }
}


@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔒",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "End-to-End Encrypted Chat",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Messages are encrypted and stored securely",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Send a message to get started",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}




// ============ PREVIEWS ============

@Preview(name = "Empty Chat Screen", showBackground = true, showSystemUi = true)
@Composable
private fun ChatScreenEmptyPreview() {
    ChatTheme {
        Surface {
            EmptyState()
        }
    }
}

@Preview(name = "Chat Input Bar - Empty", showBackground = true)
@Composable
private fun ChatInputBarEmptyPreview() {
    ChatTheme {
        ChatInputBar(
            inputText = "",
            onInputChange = {},
            onSendClick = {},
            isSending = false
        )
    }
}

@Preview(name = "Chat Input Bar - With Text", showBackground = true)
@Composable
private fun ChatInputBarWithTextPreview() {
    ChatTheme {
        ChatInputBar(
            inputText = "Hello, this is my message!",
            onInputChange = {},
            onSendClick = {},
            isSending = false
        )
    }
}

@Preview(name = "Chat Input Bar - Sending", showBackground = true)
@Composable
private fun ChatInputBarSendingPreview() {
    ChatTheme {
        ChatInputBar(
            inputText = "Sending...",
            onInputChange = {},
            onSendClick = {},
            isSending = true
        )
    }
}

@Preview(name = "Empty State", showBackground = true)
@Composable
private fun EmptyStatePreview() {
    ChatTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            EmptyState()
        }
    }
}

@Preview(name = "Empty State - Dark", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    ChatTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            EmptyState()
        }
    }
}

@Preview(name = "Chat Screen with Messages", showSystemUi = true)
@Composable
private fun ChatScreenWithMessagesPreview() {
    val sampleMessages = listOf(
        Message(1, "Hey! How are you?", System.currentTimeMillis() - 300000, true),
        Message(2, "ok", System.currentTimeMillis() - 250000, false),
        Message(3, "Great! Want to meet up later?", System.currentTimeMillis() - 200000, true),
        Message(4, "ok", System.currentTimeMillis() - 150000, false),
        Message(5, "Awesome! See you at 5pm", System.currentTimeMillis() - 100000, true),
        Message(6, "ok", System.currentTimeMillis() - 50000, false),
    )

    ChatTheme {
        ChatScreenContent(
            messages = sampleMessages,
            inputText = "New message...",
            isSending = false,
            messageCount = sampleMessages.size,
            onInputChange = {},
            onSendClick = {},
            onInsertDataset = {}
        )
    }
}

@Preview(name = "Chat Screen with Messages - Dark", showSystemUi = true)
@Composable
private fun ChatScreenWithMessagesDarkPreview() {
    val sampleMessages = listOf(
        Message(1, "This is a sent message", System.currentTimeMillis() - 300000, true),
        Message(2, "ok", System.currentTimeMillis() - 250000, false),
        Message(3, "Another message with more text to show wrapping", System.currentTimeMillis() - 200000, true),
        Message(4, "ok", System.currentTimeMillis() - 150000, false),
    )

    ChatTheme(darkTheme = true) {
        ChatScreenContent(
            messages = sampleMessages,
            inputText = "",
            isSending = false,
            messageCount = sampleMessages.size,
            onInputChange = {},
            onSendClick = {},
            onInsertDataset = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    messages: List<Message>,
    inputText: String,
    isSending: Boolean,
    messageCount: Int,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onInsertDataset: () -> Unit
) {
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Encrypted Chat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$messageCount messages",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(onClick = onInsertDataset) {
                        Text("+ 1K")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputChange = onInputChange,
                onSendClick = onSendClick,
                isSending = isSending
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty()) {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }
    }
}