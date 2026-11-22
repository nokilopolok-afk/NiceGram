package com.nicegram.next

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicegram.next.proxy.ProxyLink
import com.nicegram.next.proxy.ProxyLinkParser
import com.nicegram.next.telegram.TelegramClientApp
import com.nicegram.next.telegram.TelegramRuntime

private val Ink = Color(0xFF090A10)
private val Panel = Color(0xFF141620)
private val PanelRaised = Color(0xFF1B1E2A)
private val Lavender = Color(0xFFB99AFF)
private val Rose = Color(0xFFFF637D)
private val Muted = Color(0xFF85899B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val telegram = TelegramRuntime.get(applicationContext)
        setContent { NiceGramTheme { TelegramClientApp(telegram) } }
    }
}

@Composable
private fun NiceGramTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lavender,
            secondary = Rose,
            background = Ink,
            surface = Panel,
            onSurface = Color(0xFFF4F2FA)
        ),
        content = content
    )
}

private enum class MainTab { Chats, Archive, Settings }

private data class Conversation(
    val name: String,
    val initials: String,
    val preview: String,
    val time: String,
    val unread: Int = 0,
    val online: Boolean = false,
    val tone: Color
)

private val demoChats = listOf(
    Conversation("Mia Chen", "M", "Sent you a voice message", "now", 2, true, Color(0xFF8F80FF)),
    Conversation("Design circle", "D", "Nikita: the new motion feels great ✨", "12:42", 4, false, Color(0xFFE783B9)),
    Conversation("Alex Morgan", "A", "That gradient is exactly right", "11:08", 0, true, Color(0xFF64BFA2)),
    Conversation("Weekend plans", "W", "You: let's do Saturday", "Yesterday", 0, false, Color(0xFFFFB46D)),
    Conversation("Product notes", "P", "Pinned message", "Tue", 0, false, Color(0xFF78A6F7))
)

@Composable
private fun NiceGramApp() {
    var selectedTab by remember { mutableStateOf(MainTab.Chats) }
    var activeChat by remember { mutableStateOf<Conversation?>(null) }
    val proxies = remember { mutableStateListOf<ProxyLink>() }

    Box(Modifier.fillMaxSize().background(Ink)) {
        AmbientBackdrop()
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (activeChat == null) {
                    BottomNavigation(selectedTab) { selectedTab = it }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = activeChat,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 18 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(180)) { -it / 24 })
                },
                label = "screen-transition"
            ) { chat ->
                if (chat != null) {
                    ChatPreviewScreen(chat, onBack = { activeChat = null })
                } else {
                    when (selectedTab) {
                        MainTab.Chats -> ChatsScreen(onOpenChat = { activeChat = it })
                        MainTab.Archive -> ArchiveScreen()
                        MainTab.Settings -> SettingsScreen(proxies)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientBackdrop() {
    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = -0.12f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ambient-drift"
    )
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x553F276F), Color.Transparent),
                center = Offset(size.width * (0.82f + drift), size.height * 0.12f),
                radius = size.width * 0.78f
            ),
            radius = size.width * 0.78f,
            center = Offset(size.width * (0.82f + drift), size.height * 0.12f)
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x33A92F55), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.6f),
                radius = size.width * 0.65f
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.05f, size.height * 0.6f)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BottomNavigation(selected: MainTab, onSelect: (MainTab) -> Unit) {
    BottomAppBar(
        containerColor = Color(0xEE10121A),
        contentPadding = PaddingValues(horizontal = 10.dp),
        windowInsets = WindowInsets.navigationBars,
        tonalElevation = 0.dp
    ) {
        MainTab.entries.forEach { tab ->
            val chosen = selected == tab
            val scale by animateFloatAsState(if (chosen) 1f else 0.92f, tween(240), label = "tab-scale")
            NavigationBarItem(
                selected = chosen,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            MainTab.Chats -> Icons.Default.ChatBubbleOutline
                            MainTab.Archive -> Icons.Default.Archive
                            MainTab.Settings -> Icons.Default.Settings
                        },
                        contentDescription = null,
                        modifier = Modifier.scale(scale)
                    )
                },
                label = { Text(tab.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Lavender,
                    selectedTextColor = Lavender,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                    indicatorColor = Color(0x332D2842)
                )
            )
        }
    }
}

private val MainTab.label: String
    get() = when (this) {
        MainTab.Chats -> "Чаты"
        MainTab.Archive -> "Архив"
        MainTab.Settings -> "Настройки"
    }

@Composable
private fun ChatsScreen(onOpenChat: (Conversation) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        demoChats.filter { it.name.contains(query, ignoreCase = true) || it.preview.contains(query, ignoreCase = true) }
    }

    Column(
        Modifier.fillMaxSize().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("NICEGRAM", color = Lavender, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
                Text("Твои диалоги", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Lavender, Rose))),
                contentAlignment = Alignment.Center
            ) {
                Text("N", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
        PreviewStatus(Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))
        SearchField(query, { query = it }, Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(20.dp))
        SectionHeading("КРУГИ", "Все")
        StoriesRow()
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Сообщения", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreHoriz, null, tint = Muted)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 22.dp)
        ) {
            items(filtered, key = { it.name }) { chat ->
                ChatRow(chat, onClick = { onOpenChat(chat) })
            }
        }
    }
}

@Composable
private fun PreviewStatus(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF171924))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFC46B)))
        Spacer(Modifier.width(10.dp))
        Text(
            "Предпросмотр · Telegram ещё не подключён",
            color = Color(0xFFCFCDDA),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(17.dp))
            .background(Color(0xAA1A1D28))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(17.dp))
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(11.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(Lavender),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("Поиск по чатам", color = Muted, fontSize = 14.sp)
                    inner()
                }
            },
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, null, tint = Muted, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, modifier = Modifier.weight(1f))
        Text(action, color = Lavender, fontSize = 12.sp)
    }
}

@Composable
private fun StoriesRow() {
    val stories = listOf("Твоя история" to "＋", "Mia" to "M", "Design" to "D", "Alex" to "A", "Nika" to "N")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        items(stories) { (label, initial) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(62.dp)) {
                val pulse by rememberInfiniteTransition(label = "story-$label").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.035f,
                    animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "story-pulse"
                )
                Box(
                    Modifier.size(58.dp).scale(if (label == "Твоя история") 1f else pulse)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Lavender, Rose)))
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF191B27)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(label, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ChatRow(chat: Conversation, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).animateContentSize(tween(280))
            .background(Color(0x70191B25)).padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(chat.tone.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Text(chat.initials, color = chat.tone, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            if (chat.online) Box(
                Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape)
                    .background(Color(0xFF76D8AC)).border(2.dp, Panel, CircleShape)
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(chat.preview, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(chat.time, color = if (chat.unread > 0) Lavender else Muted, fontSize = 10.sp)
            if (chat.unread > 0) Box(
                Modifier.clip(CircleShape).background(Brush.linearGradient(listOf(Lavender, Rose)))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) { Text(chat.unread.toString(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun ArchiveScreen() {
    Column(
        Modifier.fillMaxSize().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp,
            start = 22.dp,
            end = 22.dp
        )
    ) {
        Text("Тихий архив", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("Сохранённое остаётся только у тебя", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
        Spacer(Modifier.height(28.dp))
        SearchField("", {}, Modifier)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(82.dp).clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(Color(0x553F2E67), Color(0x332C5B87)))),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Archive, null, tint = Lavender, modifier = Modifier.size(33.dp)) }
                Text("Здесь будет твой архив", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
                Text("Подключи аккаунт и настрой локальное сохранение.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SettingsScreen(proxies: MutableList<ProxyLink>) {
    var showProxyForm by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp
        )
    ) {
        Text("Настройки", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 22.dp))
        Text("Твой аккаунт, твои правила", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 5.dp))
        Spacer(Modifier.height(20.dp))
        SettingCard(
            icon = Icons.Default.Shield,
            title = "Подключение Telegram",
            subtitle = "Интеграция TDLib ещё не настроена",
            onClick = {}
        )
        SettingCard(
            icon = Icons.Default.Settings,
            title = "MTProto-прокси",
            subtitle = if (proxies.isEmpty()) "Прокси не добавлены" else "${proxies.size} прокси сохранено локально",
            onClick = { showProxyForm = !showProxyForm }
        )
        AnimatedVisibility(showProxyForm) {
            ProxySetupCard(proxies)
        }
        SettingCard(
            icon = Icons.Default.Archive,
            title = "Приватность и архив",
            subtitle = "Локальное хранение · управление данными",
            onClick = {}
        )
        Spacer(Modifier.height(14.dp))
        Text("О ПРИЛОЖЕНИИ", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))
        Text("NiceGram Next · Preview 0.1.0", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 22.dp))
    }
}

@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(19.dp)).background(Color(0x99191B25))
            .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x263F3465)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Lavender, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}

@Composable
private fun ProxySetupCard(proxies: MutableList<ProxyLink>) {
    var link by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lastAdded by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp)).background(PanelRaised).padding(16.dp)
    ) {
        Text("Добавить прокси", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text("Вставь ссылку t.me/proxy. Сейчас параметры сохраняются только в памяти приложения.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 12.dp))
        TextField(
            value = link,
            onValueChange = { link = it; error = null; lastAdded = false },
            placeholder = { Text("https://t.me/proxy?server=…", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.ContentPaste, null, tint = Muted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF11131B),
                unfocusedContainerColor = Color(0xFF11131B),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(error != null) {
            Text(error.orEmpty(), color = Color(0xFFFF8F9B), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
        AnimatedVisibility(lastAdded) {
            Text("Прокси сохранён локально. Подключение появится после интеграции TDLib.", color = Color(0xFF9ED8B8), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        FilledIconButton(
            onClick = {
                when (val parsed = ProxyLinkParser.parse(link)) {
                    is ProxyLinkParser.Result.Success -> {
                        if (proxies.none { it.server == parsed.proxy.server && it.port == parsed.proxy.port }) {
                            proxies.add(parsed.proxy)
                        }
                        lastAdded = true
                    }
                    is ProxyLinkParser.Result.Error -> error = parsed.message
                }
            },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(7.dp))
            Text("Сохранить прокси")
        }
    }
}

@Composable
private fun ChatPreviewScreen(chat: Conversation, onBack: () -> Unit) {
    var message by remember { mutableStateOf("") }
    val sentMessages = remember { mutableStateListOf<String>() }
    Column(
        Modifier.fillMaxSize().padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = 8.dp
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White) }
            Avatar(chat, 42)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(chat.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(if (chat.online) "в сети" else "демо-переписка", color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = Muted) }
        }
        PreviewStatus(Modifier.padding(horizontal = 16.dp))
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { MessageBubble("Привет! Как тебе новый интерфейс?", "12:40", false) }
            item { MessageBubble("Очень красиво. Особенно эти мягкие переходы ✨", "12:41", true) }
            item { MessageBubble("Хочу, чтобы всё ощущалось легко и живо.", "12:42", false) }
            items(sentMessages) { text -> MessageBubble(text, "сейчас", true) }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp)).background(PanelRaised)
                .padding(start = 16.dp, end = 7.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                cursorBrush = SolidColor(Lavender),
                decorationBox = { inner ->
                    Box {
                        if (message.isEmpty()) Text("Сообщение…", color = Muted, fontSize = 14.sp)
                        inner()
                    }
                }
            )
            FilledIconButton(
                onClick = { if (message.isNotBlank()) { sentMessages.add(message.trim()); message = "" } },
                enabled = message.isNotBlank(),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Отправить", modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun MessageBubble(text: String, time: String, outgoing: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.fillMaxWidth(0.82f).clip(
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (outgoing) 18.dp else 5.dp,
                    bottomEnd = if (outgoing) 5.dp else 18.dp
                )
            )
                .background(if (outgoing) Brush.linearGradient(listOf(Color(0xFF8A4F9F), Color(0xFFB74764))) else SolidColor(PanelRaised))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text, color = Color.White, fontSize = 14.sp)
            Text(time, color = Color(0xFFDED8EC), fontSize = 9.sp, modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
        }
    }
}

@Composable
private fun Avatar(chat: Conversation, size: Int) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(chat.tone.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) { Text(chat.initials, color = chat.tone, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}
