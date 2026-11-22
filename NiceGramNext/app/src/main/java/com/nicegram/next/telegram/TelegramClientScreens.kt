package com.nicegram.next.telegram

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.nicegram.next.proxy.ProxyLinkParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ClientInk = Color(0xFF090910)
private val ClientPanel = Color(0xFF171521)
private val ClientCard = Color(0xFF201C2B)
private val ClientViolet = Color(0xFFC09AFF)
private val ClientRed = Color(0xFFFF657A)
private val ClientMuted = Color(0xFF9B96A8)

private enum class ClientTab { Chats, Archive, Settings }

@Composable
fun TelegramClientApp(repository: TelegramRepository) {
    val auth by repository.authState.collectAsState()
    val notice by repository.notice.collectAsState()
    var selectedTab by remember { mutableStateOf(ClientTab.Chats) }
    var openChat by remember { mutableStateOf<TelegramChat?>(null) }

    Box(Modifier.fillMaxSize().background(ClientInk)) {
        ClientBackdrop()
        AnimatedContent(
            targetState = auth,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it / 22 }) togetherWith
                    (fadeOut() + slideOutHorizontally { -it / 22 })
            },
            label = "telegram-auth-transition"
        ) { state ->
            when (state) {
                TelegramAuthState.Starting -> CenteredStatus("Запускаем защищённое соединение…", loading = true)
                TelegramAuthState.MissingCredentials -> MissingCredentialsScreen()
                TelegramAuthState.Ready -> {
                    AnimatedContent(
                        targetState = openChat,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { width -> if (targetState != null) width / 12 else -width / 12 }) togetherWith
                                (fadeOut() + slideOutHorizontally { width -> if (targetState != null) -width / 12 else width / 12 })
                        },
                        label = "chat-navigation"
                    ) { chat ->
                        if (chat != null) {
                            LiveChatScreen(
                                repository = repository,
                                chat = chat,
                                onBack = {
                                    openChat = null
                                    repository.closeChat()
                                }
                            )
                        } else {
                            LiveHome(
                                repository = repository,
                                selectedTab = selectedTab,
                                onSelectTab = { selectedTab = it },
                                onOpenChat = {
                                    openChat = it
                                    repository.openChat(it)
                                },
                                notice = notice
                            )
                        }
                    }
                }
                is TelegramAuthState.Phone -> AuthForm(
                    title = "Твоя Telegram",
                    detail = "Войди по номеру телефона. Код придёт в Telegram или по SMS.",
                    label = "Номер телефона",
                    hint = "+7 900 000-00-00",
                    error = state.error,
                    button = "Продолжить",
                    keyboard = androidx.compose.ui.text.input.KeyboardType.Phone,
                    onSubmit = repository::submitPhone
                )
                is TelegramAuthState.Code -> AuthForm(
                    title = "Код подтверждения",
                    detail = "Введи код, который Telegram отправил на твои устройства.",
                    label = "Код",
                    hint = "12345",
                    error = state.error,
                    button = "Подтвердить",
                    keyboard = androidx.compose.ui.text.input.KeyboardType.Number,
                    onSubmit = repository::submitCode
                )
                is TelegramAuthState.Password -> AuthForm(
                    title = "Облачный пароль",
                    detail = state.hint.takeIf(String::isNotBlank)?.let { "Подсказка: $it" }
                        ?: "Введи пароль двухэтапной проверки Telegram.",
                    label = "Пароль",
                    hint = "Пароль 2FA",
                    error = state.error,
                    button = "Войти",
                    password = true,
                    onSubmit = repository::submitPassword
                )
                is TelegramAuthState.Registration -> RegistrationForm(state.error, repository::submitRegistration)
                is TelegramAuthState.Email -> AuthForm(
                    title = "Почта для входа",
                    detail = "Telegram запросил адрес электронной почты.",
                    label = "Электронная почта",
                    hint = "name@example.com",
                    error = state.error,
                    button = "Продолжить",
                    keyboard = androidx.compose.ui.text.input.KeyboardType.Email,
                    onSubmit = repository::submitEmail
                )
                is TelegramAuthState.EmailCode -> AuthForm(
                    title = "Проверь почту",
                    detail = "Введи код подтверждения из письма.",
                    label = "Код из письма",
                    hint = "12345",
                    error = state.error,
                    button = "Подтвердить",
                    keyboard = androidx.compose.ui.text.input.KeyboardType.Number,
                    onSubmit = repository::submitEmailCode
                )
                is TelegramAuthState.OtherDevice -> OtherDeviceScreen(state.link)
                is TelegramAuthState.Unsupported -> CenteredStatus(state.description, loading = false)
                is TelegramAuthState.Failed -> CenteredStatus(state.description, loading = false)
            }
        }
    }
}

@Composable
private fun ClientBackdrop() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0x493F276F), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(900f, 60f),
                radius = 1100f
            )
        )
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0x33A92F55), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(30f, 1200f),
                radius = 980f
            )
        )
    )
}

@Composable
private fun MissingCredentialsScreen() {
    CenteredPanel(
        title = "Нужны ключи Telegram",
        detail = "Задай NICEGRAM_API_ID и NICEGRAM_API_HASH в локальном local.properties или GitHub Actions Secrets. Получить их можно на my.telegram.org. Не отправляй эти значения в чат."
    )
}

@Composable
private fun OtherDeviceScreen(link: String) {
    CenteredPanel(
        title = "Подтверди вход",
        detail = "Telegram ждёт подтверждения на другом устройстве:\n$link"
    )
}

@Composable
private fun CenteredStatus(text: String, loading: Boolean) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                CircularProgressIndicator(color = ClientViolet)
                Spacer(Modifier.height(18.dp))
            }
            Text(text, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CenteredPanel(title: String, detail: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(ClientPanel).border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NICEGRAM", color = ClientViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
            Text(detail, color = ClientMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun AuthForm(
    title: String,
    detail: String,
    label: String,
    hint: String,
    error: String?,
    button: String,
    keyboard: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    password: Boolean = false,
    onSubmit: (String) -> Unit
) {
    var value by remember(title) { mutableStateOf("") }
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(ClientPanel).border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            Text("NICEGRAM", color = ClientViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
            Text(detail, color = ClientMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp, bottom = 22.dp))
            Text(label, color = ClientMuted, fontSize = 12.sp)
            TextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(hint, color = ClientMuted) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboard),
                visualTransformation = if (password) {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                } else {
                    androidx.compose.ui.text.input.VisualTransformation.None
                },
                shape = RoundedCornerShape(16.dp),
                colors = authFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp)
            )
            AnimatedVisibility(error != null) {
                Text(error.orEmpty(), color = ClientRed, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
            GradientButton(button, enabled = value.isNotBlank(), onClick = { onSubmit(value) }, modifier = Modifier.padding(top = 18.dp))
            Text(
                "Код и пароль отправляются напрямую в Telegram через TDLib.",
                color = ClientMuted,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 13.dp)
            )
        }
    }
}

@Composable
private fun RegistrationForm(error: String?, onSubmit: (String, String) -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Создать аккаунт", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("Укажи имя, которое увидят собеседники.", color = ClientMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        TextField(firstName, { firstName = it }, label = { Text("Имя") }, singleLine = true, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        TextField(lastName, { lastName = it }, label = { Text("Фамилия (необязательно)") }, singleLine = true, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())
        AnimatedVisibility(error != null) { Text(error.orEmpty(), color = ClientRed, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
        GradientButton("Завершить регистрацию", firstName.isNotBlank(), { onSubmit(firstName, lastName) }, Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun authFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = ClientCard,
    unfocusedContainerColor = ClientCard,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = ClientViolet
)

@Composable
private fun GradientButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) Brush.linearGradient(listOf(ClientViolet, ClientRed))
                else Brush.linearGradient(listOf(Color(0xFF494451), Color(0xFF494451)))
            )
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun LiveHome(
    repository: TelegramRepository,
    selectedTab: ClientTab,
    onSelectTab: (ClientTab) -> Unit,
    onOpenChat: (TelegramChat) -> Unit,
    notice: String?
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xEE111018),
                windowInsets = WindowInsets.navigationBars,
                tonalElevation = 0.dp
            ) {
                ClientTab.entries.forEach { tab ->
                    val chosen = selectedTab == tab
                    NavigationBarItem(
                        selected = chosen,
                        onClick = { onSelectTab(tab) },
                        icon = {
                            Icon(
                                when (tab) {
                                    ClientTab.Chats -> Icons.Default.ChatBubbleOutline
                                    ClientTab.Archive -> Icons.Default.Archive
                                    ClientTab.Settings -> Icons.Default.Settings
                                },
                                contentDescription = null
                            )
                        },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ClientViolet,
                            selectedTextColor = ClientViolet,
                            unselectedIconColor = ClientMuted,
                            unselectedTextColor = ClientMuted,
                            indicatorColor = Color(0x332D2842)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp,
                    bottom = 12.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("NICEGRAM", color = ClientViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(selectedTab.label, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                }
                val connection by repository.connectionState.collectAsState()
                Text(connection, color = if (connection == "В сети") Color(0xFF87D8AC) else ClientMuted, fontSize = 11.sp)
            }
            AnimatedVisibility(notice != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp)).background(ClientCard).clickable { repository.clearNotice() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(notice.orEmpty(), color = Color(0xFFE6DDF2), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Close, null, tint = ClientMuted, modifier = Modifier.size(16.dp))
                }
            }
            when (selectedTab) {
                ClientTab.Chats -> LiveChats(repository, onOpenChat)
                ClientTab.Archive -> ArchiveScreen(repository)
                ClientTab.Settings -> SettingsScreen(repository)
            }
        }
    }
}

private val ClientTab.label: String
    get() = when (this) {
        ClientTab.Chats -> "Чаты"
        ClientTab.Archive -> "Архив"
        ClientTab.Settings -> "Настройки"
    }

@Composable
private fun LiveChats(repository: TelegramRepository, onOpenChat: (TelegramChat) -> Unit) {
    val chats by repository.chats.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(chats, query) {
        chats.filter { it.title.contains(query, true) || it.preview.contains(query, true) }
    }
    SearchBox(query, { query = it }, Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
    if (filtered.isEmpty()) {
        EmptyState(
            title = if (query.isBlank()) "Чатов пока нет" else "Ничего не найдено",
            detail = if (query.isBlank()) "Когда Telegram загрузит диалоги, они появятся здесь." else "Измени поисковый запрос."
        )
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        ) {
            items(filtered, key = { it.id }) { chat ->
                LiveChatRow(chat, onClick = { onOpenChat(chat) })
            }
        }
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp))
            .background(ClientPanel).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = ClientMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            cursorBrush = SolidColor(ClientViolet),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) Text("Поиск по чатам", color = ClientMuted, fontSize = 14.sp)
                    inner()
                }
            },
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) IconButton(onClick = { onChange("") }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, null, tint = ClientMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun LiveChatRow(chat: TelegramChat, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(18.dp))
            .background(Color(0x991A1822)).clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(ClientViolet.copy(alpha = 0.35f), ClientRed.copy(alpha = 0.35f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(chat.title.firstOrNull()?.uppercase() ?: "T", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(chat.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (chat.isDeveloper) DeveloperBadge()
            }
            Text(chat.preview.ifBlank { "Нет сообщений" }, color = ClientMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatTime(chat.timestamp), color = ClientMuted, fontSize = 10.sp)
            if (chat.unreadCount > 0) {
                Box(Modifier.clip(CircleShape).background(Brush.linearGradient(listOf(ClientViolet, ClientRed))).padding(horizontal = 7.dp, vertical = 3.dp)) {
                    Text(chat.unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LiveChatScreen(repository: TelegramRepository, chat: TelegramChat, onBack: () -> Unit) {
    val messages by repository.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp,
            bottom = 8.dp
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(chat.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (chat.isDeveloper) DeveloperBadge()
                }
                val connection by repository.connectionState.collectAsState()
                Text(connection, color = ClientMuted, fontSize = 10.sp)
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item { Text("Загружаем историю…", color = ClientMuted, modifier = Modifier.padding(12.dp)) }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(message)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(22.dp)).background(ClientCard).padding(start = 15.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f).padding(vertical = 11.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                cursorBrush = SolidColor(ClientViolet),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) Text("Сообщение…", color = ClientMuted, fontSize = 14.sp)
                        inner()
                    }
                }
            )
            FilledIconButton(
                onClick = {
                    repository.sendMessage(chat.id, text)
                    text = ""
                },
                enabled = text.isNotBlank(),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Отправить", modifier = Modifier.size(17.dp)) }
        }
    }
}

@Composable
private fun MessageBubble(message: TelegramMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.fillMaxWidth(0.82f).clip(
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.outgoing) 18.dp else 5.dp,
                    bottomEnd = if (message.outgoing) 5.dp else 18.dp
                )
            )
                .background(
                if (message.deletedAt != null) Brush.linearGradient(listOf(Color(0xFF282630), Color(0xFF282630)))
                else if (message.outgoing) Brush.linearGradient(listOf(Color(0xFF8A4F9F), Color(0xFFB74764)))
                else Brush.linearGradient(listOf(ClientCard, ClientCard))
            )
            .padding(horizontal = 13.dp, vertical = 9.dp)
        ) {
            if (message.sender.isNotBlank() && !message.outgoing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message.sender, color = ClientViolet, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    if (message.isDeveloper) DeveloperBadge()
                }
            }
            if (message.deletedAt != null) {
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = ClientMuted, modifier = Modifier.size(14.dp))
                    Text("Сообщение удалено", color = ClientMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                }
                Text(message.text, color = ClientMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                Text(
                    "Удалено ${formatTime(message.deletedAt)}",
                    color = ClientMuted,
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            } else {
                Text(message.text, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                Text(formatTime(message.timestamp), color = Color(0xFFD8CCDF), fontSize = 9.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ArchiveScreen(repository: TelegramRepository) {
    val entries by repository.deletedMessages.collectAsState()
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        SearchBox(query, { query = it }, Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        val filtered = remember(entries, query) {
            entries.filter { it.chatTitle.contains(query, true) || it.text.contains(query, true) }
        }
        if (filtered.isEmpty()) {
            EmptyState(
                title = "Архив пока пуст",
                detail = "Включи локальное сохранение в настройках. Будут доступны только входящие сообщения, полученные после включения."
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { "${it.chatId}:${it.messageId}" }) { entry ->
                        val clipboard = LocalClipboardManager.current
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(ClientPanel)
                                .padding(start = 14.dp, end = 5.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry.chatTitle.ifBlank { "Чат" }, color = ClientViolet, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(formatTime(entry.timestamp.toInt()), color = ClientMuted, fontSize = 10.sp)
                                }
                                Text(entry.text, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 7.dp))
                                Text("Удалённое сообщение · ${entry.sender}", color = ClientRed, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp))
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString(entry.text)) }) {
                                Icon(Icons.Default.ContentCopy, "Скопировать текст", tint = ClientMuted)
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun SettingsScreen(repository: TelegramRepository) {
    val archiveEnabled by repository.archiveEnabled.collectAsState()
    var showProxyForm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        SettingRow(
            icon = Icons.Default.Archive,
            title = "Локальный архив",
            subtitle = "Сохранять новые входящие сообщения на устройстве",
            trailing = { Switch(checked = archiveEnabled, onCheckedChange = repository::setArchiveEnabled) }
        )
        SettingRow(
            icon = Icons.Default.Shield,
            title = "MTProto-прокси",
            subtitle = "Добавить прокси в настройки TDLib",
            onClick = { showProxyForm = !showProxyForm }
        )
        AnimatedVisibility(showProxyForm) {
            ProxyForm(repository)
        }
        Text(
            "Архив хранится локально. Он не восстанавливает сообщения, полученные до включения функции, и не обходит удаление на стороне Telegram.",
            color = ClientMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp)
        )
    }
}

@Composable
private fun ProxyForm(repository: TelegramRepository) {
    var link by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp)).background(ClientCard).padding(14.dp)
    ) {
        Text("Ссылка t.me/proxy", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        TextField(
            value = link,
            onValueChange = { link = it; error = null },
            placeholder = { Text("https://t.me/proxy?server=…", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.ContentPaste, null, tint = ClientMuted) },
            singleLine = true,
            shape = RoundedCornerShape(13.dp),
            colors = authFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        AnimatedVisibility(error != null) {
            Text(error.orEmpty(), color = ClientRed, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
        GradientButton("Добавить и подключить", link.isNotBlank(), {
            when (val result = ProxyLinkParser.parse(link)) {
                is ProxyLinkParser.Result.Success -> {
                    repository.addProxy(result.proxy)
                    link = ""
                }
                is ProxyLinkParser.Result.Error -> error = result.message
            }
        }, Modifier.padding(top = 11.dp))
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp)).background(Color(0x991A1822))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0x293F3465)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = ClientViolet, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = ClientMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
        trailing?.invoke()
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(Color(0x443F276F)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ChatBubbleOutline, null, tint = ClientViolet, modifier = Modifier.size(30.dp))
            }
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 17.dp))
            Text(detail, color = ClientMuted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun DeveloperBadge() {
    Box(
        Modifier.padding(start = 5.dp).size(16.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(ClientViolet, ClientRed))),
        contentAlignment = Alignment.Center
    ) {
        Text("D", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(timestamp: Int): String {
    if (timestamp <= 0) return ""
    val date = Date(timestamp.toLong() * 1000)
    val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val messageDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
    return SimpleDateFormat(if (today == messageDay) "HH:mm" else "dd.MM", Locale.getDefault()).format(date)
}
