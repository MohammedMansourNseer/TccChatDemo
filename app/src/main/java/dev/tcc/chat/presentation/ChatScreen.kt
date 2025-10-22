package dev.tcc.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.tcc.chat.R
import dev.tcc.chat.domain.model.Message
import dev.tcc.chat.presentation.chat.component.MessageBubble
import dev.tcc.chat.presentation.chat.contract.ChatIntent
import dev.tcc.chat.ui.theme.ChatTheme
import dev.tcc.chat.utililty.Constant.ADD_BULK_DATA
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    usePagination: Boolean = true
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messagesPaged = viewModel.messagesPaged.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    LaunchedEffect(state.scrollToBottomTick, usePagination, messagesPaged.itemCount, state.messages.size) {
        if (state.scrollToBottomTick == 0L) return@LaunchedEffect

        if (usePagination) {
            if (messagesPaged.itemCount > 0) {
                listState.scrollToItem(0)
            }
        } else {
            if (state.messages.isNotEmpty()) {
                listState.scrollToItem(state.messages.size - 1)
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
                            text = stringResource(R.string.encrypted_chat_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(
                        onClick = { viewModel.handleIntent(ChatIntent.ClearAllMessages) }
                    ) { Text(stringResource(R.string.clear_label)) }

                    TextButton(
                        onClick = { viewModel.handleIntent(ChatIntent.InsertLargeDataset(ADD_BULK_DATA)) }
                    ) { Text(stringResource(R.string.insert_1K_label)) }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = state.inputText,
                onInputChange = { viewModel.handleIntent(ChatIntent.UpdateInputText(it)) },
                onSendClick = { viewModel.handleIntent(ChatIntent.SendMessage(state.inputText)) },
                isSending = state.isSending,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .navigationBarsPadding(),

            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (usePagination) {
                PaginatedMessageList(
                    messages = messagesPaged,
                    listState = listState,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (state.isLoading && state.messages.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else if (state.messages.isEmpty()) {
                    EmptyState(Modifier.align(Alignment.Center))
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
            }

            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) { Text(error) }
            }

            state.insertProgress?.let { progress ->
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = stringResource(R.string.inserting_messages),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.progress_percent, progress),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaginatedMessageList(
    messages: LazyPagingItems<Message>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            messages.loadState.refresh is LoadState.Loading && messages.itemCount == 0 -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            messages.loadState.refresh is LoadState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.failed_to_load_messages),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { messages.retry() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            messages.itemCount == 0 && messages.loadState.refresh !is LoadState.Loading -> {
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    reverseLayout = true
                ) {
                    if (messages.loadState.append is LoadState.Loading) {
                        item(key = "append_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    if (messages.loadState.append is LoadState.Error) {
                        item(key = "append_error") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.failed_to_load_older_messages),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { messages.retry() }) {
                                    Text(stringResource(R.string.retry), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    items(
                        count = messages.itemCount,
                        key = messages.itemKey { msg -> msg.id }
                    ) { index ->
                        messages[index]?.let { message ->
                            MessageBubble(message = message)
                        }
                    }

                    if (messages.loadState.prepend is LoadState.Loading) {
                        item(key = "prepend_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    if (messages.loadState.prepend is LoadState.Error) {
                        item(key = "prepend_error") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.failed_to_load_newer_messages),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { messages.retry() }) {
                                    Text(stringResource(R.string.retry), fontSize = 12.sp)
                                }
                            }
                        }
                    }
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
                placeholder = { Text(stringResource(R.string.type_message_placeholder)) },
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
                        contentDescription = stringResource(R.string.send_message_cd)
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
        Text(text = stringResource(R.string.empty_icon), fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(R.string.empty_subtitle_1),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(R.string.empty_subtitle_2),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


@Preview(name = "Empty Chat Screen", showBackground = true, showSystemUi = true)
@Composable
private fun ChatScreenEmptyPreview() {
    ChatTheme { Surface { EmptyState() } }
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
    ChatTheme { Surface(Modifier.fillMaxSize()) { EmptyState() } }
}

@Preview(name = "Empty State - Dark", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    ChatTheme(darkTheme = true) { Surface(Modifier.fillMaxSize()) { EmptyState() } }
}
