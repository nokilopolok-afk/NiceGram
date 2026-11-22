package com.nicegram.next.telegram

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.graphics.BitmapFactory
import android.widget.VideoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

private enum class ClientTab { Chats, Archive, Profile, Settings }

@Composable
fun TelegramClientApp(repository: TelegramRepository) {
    val auth by repository.authState.collectAsState()
    val notice by repository.notice.collectAsState()
    var selectedTab by remember { mutableStateOf(ClientTab.Chats) }
    var openChat by remember { mutableStateOf<TelegramChat?>(null) }
    var selectedStory by remember { mutableStateOf<TelegramStory?>(null) }
    val storyPresentation by repository.openStory.collectAsState()

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
                    Box(Modifier.fillMaxSize()) {
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
                                    onOpenStory = {
                                        selectedStory = it
                                        repository.showStory(it)
                                    },
                                    notice = notice
                                )
                            }
                        }
                        if (storyPresentation != null && selectedStory != null) {
                            StoryViewer(
                                presentation = storyPresentation!!,
                                onClose = {
                                    repository.closeStory(selectedStory)
                                    selectedStory = null
                                }
                            )
                        }
                    }
                }
                is TelegramAuthState.Phone -> AuthForm(
                    title = "Добро пожаловать в NiceGram",
                    detail = "Введите номер телефона, чтобы войти или создать аккаунт. Код подтверждения отправит Telegram.",
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
    val transition = rememberInfiniteTransition(label = "login-mark")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800), repeatMode = RepeatMode.Reverse),
        label = "login-mark-pulse"
    )
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(ClientPanel).border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            BrandMark(size = 66.dp, pulse = pulse)
            Text("NICEGRAM", color = ClientViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(top = 18.dp))
            Text(title, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 7.dp))
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
        AuthMark()
        Text("Создать аккаунт", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
        Text("Укажи имя, которое увидят собеседники.", color = ClientMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        TextField(firstName, { firstName = it }, label = { Text("Имя") }, singleLine = true, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        TextField(lastName, { lastName = it }, label = { Text("Фамилия (необязательно)") }, singleLine = true, colors = authFieldColors(), modifier = Modifier.fillMaxWidth())
        AnimatedVisibility(error != null) { Text(error.orEmpty(), color = ClientRed, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
        GradientButton("Завершить регистрацию", firstName.isNotBlank(), { onSubmit(firstName, lastName) }, Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun AuthMark() {
    val transition = rememberInfiniteTransition(label = "registration-mark")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800), repeatMode = RepeatMode.Reverse),
        label = "registration-mark-pulse"
    )
    BrandMark(size = 66.dp, pulse = pulse)
}

@Composable
private fun BrandMark(size: Dp, pulse: Float) {
    val context = LocalContext.current
    val avatar by produceState<ImageBitmap?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            try {
                context.assets.open("nicegram_avatar.png").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: java.io.IOException) {
                null
            }
        }
    }
    Box(
        Modifier.size(size).graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(ClientViolet, ClientRed))),
        contentAlignment = Alignment.Center
    ) {
        if (avatar != null) {
            Image(
                bitmap = avatar!!,
                contentDescription = "Аватар NiceGram",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(size * 0.43f))
        }
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
    onOpenStory: (TelegramStory) -> Unit,
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
                                    ClientTab.Profile -> Icons.Default.Person
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
                ClientTab.Chats -> LiveChats(repository, onOpenChat, onOpenStory)
                ClientTab.Archive -> ArchiveScreen(repository)
                ClientTab.Profile -> ProfileScreen(repository, onOpenStory)
                ClientTab.Settings -> SettingsScreen(repository) {
                    onSelectTab(ClientTab.Profile)
                }
            }
        }
    }
}

private val ClientTab.label: String
    get() = when (this) {
        ClientTab.Chats -> "Чаты"
        ClientTab.Archive -> "Архив"
        ClientTab.Profile -> "Профиль"
        ClientTab.Settings -> "Настройки"
    }

@Composable
private fun LiveChats(
    repository: TelegramRepository,
    onOpenChat: (TelegramChat) -> Unit,
    onOpenStory: (TelegramStory) -> Unit
) {
    val chats by repository.chats.collectAsState()
    val folders by repository.folders.collectAsState()
    val selectedFolder by repository.selectedFolderId.collectAsState()
    val stories by repository.stories.collectAsState()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(chats.isNotEmpty(), stories.isEmpty()) {
        if (chats.isNotEmpty() && stories.isEmpty()) repository.refreshStories()
    }
    val filtered = remember(chats, query) {
        chats.filter { it.title.contains(query, true) || it.preview.contains(query, true) }
    }
    LazyRow(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 3.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFolder == null,
                onClick = { repository.selectFolder(null) },
                label = { Text("Все") }
            )
        }
        items(folders, key = { it.id }) { folder ->
            FilterChip(
                selected = selectedFolder == folder.id,
                onClick = { repository.selectFolder(folder.id) },
                label = { Text(folder.title, maxLines = 1) }
            )
        }
    }
    if (stories.isNotEmpty()) StoriesStrip(repository, stories, onOpenStory)
    else {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Истории", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "Обновить",
                color = ClientViolet,
                fontSize = 12.sp,
                modifier = Modifier.clickable { repository.refreshStories() }.padding(7.dp)
            )
        }
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
                LiveChatRow(chat, repository, onClick = { onOpenChat(chat) })
            }
        }
    }
}

@Composable
private fun StoriesStrip(
    repository: TelegramRepository,
    stories: List<TelegramStory>,
    onOpenStory: (TelegramStory) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 7.dp)) {
        Text("Истории", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, bottom = 7.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stories, key = { "${it.chatId}:${it.storyId}" }) { story ->
                Column(
                    Modifier.width(70.dp).clickable { onOpenStory(story) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ClientViolet, ClientRed)))
                            .padding(3.dp).clip(CircleShape).background(ClientInk),
                        contentAlignment = Alignment.Center
                    ) {
                        TelegramAvatar(
                            repository = repository,
                            fileId = story.avatarFileId,
                            label = story.chatTitle,
                            size = 52.dp
                        )
                    }
                    Text(
                        story.chatTitle,
                        color = if (story.isSeen) ClientMuted else Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(repository: TelegramRepository, onOpenStory: (TelegramStory) -> Unit) {
    val profile by repository.profile.collectAsState()
    val stories by repository.stories.collectAsState()
    val myStories = stories.filter { it.isMine }
    if (profile == null) {
        EmptyState("Профиль загружается", "Подождите, пока Telegram вернёт данные аккаунта.")
        return
    }
    val account = profile!!
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TelegramAvatar(
            repository = repository,
            fileId = account.avatarFileId,
            label = account.name,
            size = 104.dp
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
            Text(account.name.ifBlank { "Telegram" }, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            if (account.isPremium) {
                Icon(Icons.Default.Star, "Telegram Premium", tint = ClientViolet, modifier = Modifier.padding(start = 6.dp).size(18.dp))
            }
        }
        if (account.username.isNotBlank()) {
            Text("@${account.username}", color = ClientViolet, fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp))
        }
        ProfileInfoRow("Номер телефона", account.phoneNumber.ifBlank { "Не указан" })
        ProfileInfoRow("Статус", if (account.isPremium) "Telegram Premium" else "Обычный аккаунт")
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Мои истории", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "Обновить",
                color = ClientViolet,
                fontSize = 12.sp,
                modifier = Modifier.clickable { repository.refreshStories() }.padding(7.dp)
            )
        }
        if (myStories.isEmpty()) {
            Text("Активных историй не найдено.", color = ClientMuted, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        } else {
            StoriesStrip(repository, myStories, onOpenStory)
        }
    }
}

@Composable
private fun ProfileInfoRow(title: String, value: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 17.dp)
            .clip(RoundedCornerShape(16.dp)).background(ClientPanel).padding(14.dp)
    ) {
        Text(title, color = ClientMuted, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun StoryViewer(presentation: TelegramStoryPresentation, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            Modifier.fillMaxSize().padding(10.dp),
            color = Color(0xFF08070B),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(presentation.chatTitle, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(formatTime(presentation.timestamp), color = ClientMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Закрыть историю", tint = Color.White)
                    }
                }
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(18.dp)).background(ClientPanel),
                    contentAlignment = Alignment.Center
                ) {
                    val path = presentation.mediaPath
                    if (path == null) {
                        Text("Медиа истории недоступно", color = ClientMuted)
                    } else if (presentation.isVideo) {
                        LoopingVideo(path, Modifier.fillMaxSize())
                    } else {
                        LocalMediaImage(path, Modifier.fillMaxSize())
                    }
                }
                if (presentation.caption.isNotBlank()) {
                    Text(
                        presentation.caption,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalMediaImage(path: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    bitmap?.let {
        Image(it, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    } ?: Text("Загружаем медиа…", color = ClientMuted)
}

@Composable
private fun TelegramAvatar(
    repository: TelegramRepository,
    fileId: Int?,
    label: String,
    size: Dp
) {
    val path by produceState<String?>(initialValue = null, fileId) {
        value = fileId?.let { repository.downloadAvatar(it) }
    }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        }
    }
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(listOf(ClientViolet, ClientRed))),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap!!, contentDescription = "Фото профиля: $label", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(
                label.firstOrNull()?.uppercase() ?: "T",
                color = Color.White,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LoopingVideo(path: String, modifier: Modifier = Modifier) {
    var videoView by remember(path) { mutableStateOf<VideoView?>(null) }
    AndroidView(
        factory = { context ->
            VideoView(context).also { view ->
                videoView = view
                view.setVideoPath(path)
                view.setOnPreparedListener { player ->
                    player.isLooping = true
                    view.start()
                }
            }
        },
        modifier = modifier
    )
    androidx.compose.runtime.DisposableEffect(videoView) {
        onDispose { videoView?.stopPlayback() }
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
private fun LiveChatRow(chat: TelegramChat, repository: TelegramRepository, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(18.dp))
            .background(Color(0x991A1822)).clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelegramAvatar(repository, chat.avatarFileId, chat.title, 52.dp)
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
@OptIn(ExperimentalMaterial3Api::class)
private fun LiveChatScreen(repository: TelegramRepository, chat: TelegramChat, onBack: () -> Unit) {
    val messages by repository.messages.collectAsState()
    val deletedArchive by repository.deletedMessages.collectAsState()
    val chatProfile by repository.chatProfile.collectAsState()
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    var showDeletionHistory by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp,
            bottom = 8.dp
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White) }
            Column(Modifier.weight(1f).clickable { repository.loadChatProfile(chat.id) }) {
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
                MessageBubble(message, repository) {
                    if (message.deletedAt != null) showDeletionHistory = true
                }
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
    if (showDeletionHistory) {
        val entries = deletedArchive.filter { it.chatId == chat.id }
        val visibleDeletedMessages = messages.filter { it.deletedAt != null }
        ModalBottomSheet(
            onDismissRequest = { showDeletionHistory = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ClientPanel,
            contentColor = Color.White
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
                Text("История удалений", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(chat.title, color = ClientMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                if (entries.isEmpty() && visibleDeletedMessages.isEmpty()) {
                    Text(
                        "Архив не содержит удалённых сообщений из этого чата.",
                        color = ClientMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                } else if (entries.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(entries, key = { "${it.chatId}:${it.messageId}" }) { entry ->
                            DeletionHistoryRow(entry.text, entry.sender, entry.timestamp.toInt()) {
                                clipboard.setText(AnnotatedString(entry.text))
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visibleDeletedMessages, key = { it.id }) { message ->
                            DeletionHistoryRow(message.text, message.sender, message.deletedAt ?: message.timestamp) {
                                clipboard.setText(AnnotatedString(message.text))
                            }
                        }
                    }
                    chatProfile?.let { profile ->
                        ModalBottomSheet(
                            onDismissRequest = repository::closeChatProfile,
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = ClientPanel,
                            contentColor = Color.White
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TelegramAvatar(repository, profile.avatarFileId, profile.title, 92.dp)
                                Text(profile.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                                if (profile.username.isNotBlank()) {
                                    Text("@${profile.username}", color = ClientViolet, fontSize = 14.sp, modifier = Modifier.padding(top = 5.dp))
                                }
                                if (profile.phoneNumber.isNotBlank()) ProfileInfoRow("Номер телефона", profile.phoneNumber)
                                if (profile.userId != null) ProfileInfoRow("Telegram ID", profile.userId.toString())
                                if (profile.isGroup) ProfileInfoRow("Тип", "Группа или канал")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: TelegramMessage,
    repository: TelegramRepository,
    onLongPress: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().then(
            if (message.deletedAt != null) {
                Modifier.pointerInput(message.id, message.deletedAt) {
                    detectTapGestures(onLongPress = { onLongPress() })
                }
            } else {
                Modifier
            }
        ),
        horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start
    ) {
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
            } else if (message.mediaFileId != null || message.stickerEmoji != null) {
                StickerMedia(message, repository)
                Text(formatTime(message.timestamp), color = Color(0xFFD8CCDF), fontSize = 9.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            } else {
                Text(message.text, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                Text(formatTime(message.timestamp), color = Color(0xFFD8CCDF), fontSize = 9.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun StickerMedia(message: TelegramMessage, repository: TelegramRepository) {
    val fileId = message.mediaFileId
    val path by produceState<String?>(initialValue = null, fileId) {
        value = try {
            fileId?.let { repository.downloadFile(it).local.path.takeIf(String::isNotBlank) }
        } catch (_: Exception) {
            null
        }
    }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        }
    }
    val localPath = path
    if (localPath?.endsWith(".webm", ignoreCase = true) == true) {
        LoopingVideo(localPath, Modifier.size(150.dp))
    } else if (bitmap != null) {
        Image(
            bitmap!!,
            contentDescription = message.stickerEmoji ?: "Стикер",
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 7.dp)) {
            Text(message.stickerEmoji ?: "✨", color = Color.White, fontSize = 48.sp)
            Text(
                if (message.isPremiumSticker) "Premium sticker" else "Стикер",
                color = if (message.isPremiumSticker) ClientViolet else ClientMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun DeletionHistoryRow(text: String, sender: String, timestamp: Int, onCopy: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(ClientCard).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${sender} · ${formatTime(timestamp)}", color = ClientMuted, fontSize = 10.sp)
            Text(text, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, "Скопировать текст", tint = ClientViolet)
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
private fun SettingsScreen(repository: TelegramRepository, onOpenProfile: () -> Unit) {
    val archiveEnabled by repository.archiveEnabled.collectAsState()
    val profile by repository.profile.collectAsState()
    val isDeveloperAccount by repository.isDeveloperAccount.collectAsState()
    val connection by repository.connectionState.collectAsState()
    val ghostMode by repository.ghostMode.collectAsState()
    var showProxyForm by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    if (showAdminPanel) {
        AdminPanel(repository, onBack = { showAdminPanel = false })
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { SettingsSectionTitle("Аккаунт") }
        if (profile != null) {
            item {
                SettingRow(
                    icon = Icons.Default.Person,
                    title = profile!!.name.ifBlank { "Мой профиль" },
                    subtitle = listOfNotNull(
                        profile!!.username.takeIf(String::isNotBlank)?.let { "@$it" },
                        profile!!.phoneNumber.takeIf(String::isNotBlank)
                    ).joinToString(" · ").ifBlank { "Фото профиля и активные истории" },
                    onClick = onOpenProfile
                )
            }
        }
        item { SettingsSectionTitle("Чаты") }
        item {
            SettingRow(
                icon = Icons.Default.Archive,
                title = "Архив удалённых сообщений",
                subtitle = "Локально сохранять новые входящие сообщения",
                trailing = { Switch(checked = archiveEnabled, onCheckedChange = repository::setArchiveEnabled) }
            )
        }
        item { SettingsSectionTitle("Конфиденциальность") }
        item {
            SettingRow(
                icon = Icons.Default.Shield,
                title = "Режим призрака",
                subtitle = "Не показывать статус «в сети»; новые сообщения остаются непрочитанными",
                trailing = { Switch(checked = ghostMode, onCheckedChange = repository::setGhostMode) }
            )
        }
        item {
            Text(
                "Режим управляет онлайн-статусом TDLib и не меняет серверные настройки приватности Telegram.",
                color = ClientMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
            )
        }
        item { SettingsSectionTitle("Подключение") }
        item {
            SettingRow(
                icon = Icons.Default.Shield,
                title = "MTProto-прокси",
                subtitle = "Настроить подключение через TDLib",
                onClick = { showProxyForm = !showProxyForm }
            )
        }
        if (showProxyForm) item { ProxyForm(repository) }
        item {
            SettingRow(
                icon = Icons.Default.ChatBubbleOutline,
                title = "Состояние соединения",
                subtitle = connection
            )
        }
        item { SettingsSectionTitle("NiceGram") }
        if (isDeveloperAccount) {
            item {
                SettingRow(
                    icon = Icons.Default.Settings,
                    title = "Панель разработчика",
                    subtitle = "Статистика клиента, диагностика и обслуживание",
                    onClick = { showAdminPanel = true }
                )
            }
        }
        item {
            Text(
                "Официальные функции Telegram, включая приватность и истории, зависят от настроек аккаунта и сервера.",
                color = ClientMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title.uppercase(),
        color = ClientViolet,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 3.dp)
    )
}

@Composable
private fun AdminPanel(repository: TelegramRepository, onBack: () -> Unit) {
    val isDeveloperAccount by repository.isDeveloperAccount.collectAsState()
    val profile by repository.profile.collectAsState()
    val chats by repository.chats.collectAsState()
    val stories by repository.stories.collectAsState()
    val connection by repository.connectionState.collectAsState()
    val events by repository.adminEvents.collectAsState()
    var confirmCacheClear by remember { mutableStateOf(false) }

    if (!isDeveloperAccount) {
        EmptyState("Доступ запрещён", "Панель доступна только аккаунту разработчика.")
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = ClientViolet)
                }
                Column {
                    Text("Панель разработчика", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text("Локальные инструменты NiceGram", color = ClientMuted, fontSize = 11.sp)
                }
            }
        }
        item {
            SettingRow(
                icon = Icons.Default.Person,
                title = profile?.name.orEmpty().ifBlank { "Аккаунт разработчика" },
                subtitle = "ID ${profile?.id ?: "—"} · $connection"
            )
        }
        item {
            SettingRow(
                icon = Icons.Default.ChatBubbleOutline,
                title = "Загруженные чаты",
                subtitle = chats.size.toString()
            )
        }
        item {
            SettingRow(
                icon = Icons.Default.Archive,
                title = "Доступные истории",
                subtitle = stories.size.toString()
            )
        }
        item {
            SettingRow(
                icon = Icons.Default.Settings,
                title = "Обновить данные",
                subtitle = "Повторно запросить список чатов и историй",
                onClick = repository::refreshStories
            )
        }
        item {
            SettingRow(
                icon = Icons.Default.DeleteOutline,
                title = "Очистить временный кэш",
                subtitle = "Архив сообщений и данные входа останутся",
                onClick = { confirmCacheClear = true }
            )
        }
        item {
            Text(
                "Диагностические события",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp)
            )
        }
        if (events.isEmpty()) {
            item {
                Text("Событий пока нет.", color = ClientMuted, fontSize = 12.sp, modifier = Modifier.padding(4.dp))
            }
        } else {
            items(events) { event ->
                Text(
                    event,
                    color = ClientMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(ClientPanel).padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }

    if (confirmCacheClear) {
        AlertDialog(
            onDismissRequest = { confirmCacheClear = false },
            title = { Text("Очистить временный кэш?") },
            text = { Text("Будут удалены только временные файлы NiceGram. Архив сообщений, данные входа и база TDLib не затрагиваются.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCacheClear = false
                    repository.clearTemporaryCache()
                }) {
                    Text("Очистить", color = ClientRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCacheClear = false }) {
                    Text("Отмена", color = ClientViolet)
                }
            },
            containerColor = ClientPanel,
            titleContentColor = Color.White,
            textContentColor = ClientMuted
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
