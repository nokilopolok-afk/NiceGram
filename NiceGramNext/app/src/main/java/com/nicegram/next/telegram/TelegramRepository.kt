package com.nicegram.next.telegram

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.nicegram.next.BuildConfig
import com.nicegram.next.proxy.ProxyLink
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

sealed interface TelegramAuthState {
    data object Starting : TelegramAuthState
    data object MissingCredentials : TelegramAuthState
    data class Phone(val error: String? = null) : TelegramAuthState
    data class Code(val error: String? = null) : TelegramAuthState
    data class Password(val hint: String, val error: String? = null) : TelegramAuthState
    data class Registration(val error: String? = null) : TelegramAuthState
    data class Email(val error: String? = null) : TelegramAuthState
    data class EmailCode(val error: String? = null) : TelegramAuthState
    data class OtherDevice(val link: String) : TelegramAuthState
    data class Unsupported(val description: String) : TelegramAuthState
    data class Failed(val description: String) : TelegramAuthState
    data object Ready : TelegramAuthState
}

data class TelegramChat(
    val id: Long,
    val title: String,
    val preview: String,
    val timestamp: Int,
    val unreadCount: Int,
    val isGroup: Boolean,
    val isDeveloper: Boolean
)

data class TelegramMessage(
    val id: Long,
    val chatId: Long,
    val sender: String,
    val text: String,
    val timestamp: Int,
    val outgoing: Boolean,
    val isDeveloper: Boolean,
    val deletedAt: Int? = null
)

object TelegramRuntime {
    @Volatile
    private var instance: TelegramRepository? = null

    fun get(context: Context): TelegramRepository = instance ?: synchronized(this) {
        instance ?: TelegramRepository(context.applicationContext).also { instance = it }
    }
}

class TelegramRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = Room.databaseBuilder(
        appContext,
        TelegramArchiveDatabase::class.java,
        "nicegram-archive.db"
    ).build()
    private val secureStorage = DeviceSecureStorage(appContext)
    private val updates = Channel<TdApi.Object>(Channel.UNLIMITED)

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Starting)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private val _chats = MutableStateFlow<List<TelegramChat>>(emptyList())
    val chats: StateFlow<List<TelegramChat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<TelegramMessage>>(emptyList())
    val messages: StateFlow<List<TelegramMessage>> = _messages.asStateFlow()

    private val _archiveEnabled = MutableStateFlow(
        appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(KEY_ARCHIVE_ENABLED, false)
    )
    val archiveEnabled: StateFlow<Boolean> = _archiveEnabled.asStateFlow()

    private val _deletedMessages = MutableStateFlow<List<ArchivedMessage>>(emptyList())
    val deletedMessages: StateFlow<List<ArchivedMessage>> = _deletedMessages.asStateFlow()

    private val _connectionState = MutableStateFlow("Подключаемся")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val chatObjects = linkedMapOf<Long, TdApi.Chat>()
    private val knownMessages = mutableMapOf<Long, MutableList<TelegramMessage>>()
    private var activeChatId: Long? = null
    private var client: Client? = null
    private var me: TdApi.User? = null

    init {
        scope.launch {
            for (update in updates) {
                try {
                    handleUpdate(update)
                } catch (error: Exception) {
                    _notice.value = error.message ?: "Не удалось обработать обновление Telegram."
                    if (_authState.value == TelegramAuthState.Starting) {
                        _authState.value = TelegramAuthState.Failed(
                            error.message ?: "Не удалось подготовить безопасную базу Telegram."
                        )
                    }
                }
            }
        }
        scope.launch {
            database.archiveDao().observeDeleted().collect { entries ->
                try {
                    _deletedMessages.value = entries.map { entry ->
                        ArchivedMessage(
                            chatId = entry.chatId,
                            messageId = entry.messageId,
                            chatTitle = entry.chatTitle,
                            sender = entry.sender,
                            text = secureStorage.decrypt(entry.encryptedText),
                            timestamp = entry.timestamp
                        )
                    }
                } catch (error: Exception) {
                    _notice.value = error.message ?: "Не удалось расшифровать локальный архив."
                }
            }
        }
        if (BuildConfig.TELEGRAM_API_ID <= 0 || BuildConfig.TELEGRAM_API_HASH.isBlank()) {
            _authState.value = TelegramAuthState.MissingCredentials
        } else {
            initializeClient()
        }
    }

    fun setArchiveEnabled(enabled: Boolean) {
        _archiveEnabled.value = enabled
        appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ARCHIVE_ENABLED, enabled)
            .apply()
        _notice.value = if (enabled) {
            "Будут сохраняться только входящие сообщения, полученные после включения архива."
        } else {
            "Сбор новых сообщений остановлен. Уже сохранённый архив не удалён."
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    fun submitPhone(phone: String) {
        if (phone.isBlank()) {
            _authState.value = TelegramAuthState.Phone("Введи номер телефона.")
            return
        }
        runAuthAction(TelegramAuthState.Phone(), "SetAuthenticationPhoneNumber") {
            request(TdApi.SetAuthenticationPhoneNumber(phone.trim(), null))
        }
    }

    fun submitCode(code: String) {
        if (code.isBlank()) {
            _authState.value = TelegramAuthState.Code("Введи код из Telegram.")
            return
        }
        runAuthAction(TelegramAuthState.Code(), "CheckAuthenticationCode") {
            request(TdApi.CheckAuthenticationCode(code.trim()))
        }
    }

    fun submitPassword(password: String) {
        if (password.isBlank()) {
            val hint = (authState.value as? TelegramAuthState.Password)?.hint.orEmpty()
            _authState.value = TelegramAuthState.Password(hint, "Введи пароль двухэтапной проверки.")
            return
        }
        val hint = (authState.value as? TelegramAuthState.Password)?.hint.orEmpty()
        runAuthAction(TelegramAuthState.Password(hint), "CheckAuthenticationPassword") {
            request(TdApi.CheckAuthenticationPassword(password))
        }
    }

    fun submitRegistration(firstName: String, lastName: String) {
        if (firstName.isBlank()) {
            _authState.value = TelegramAuthState.Registration("Введи имя.")
            return
        }
        runAuthAction(TelegramAuthState.Registration(), "RegisterUser") {
            request(TdApi.RegisterUser(firstName.trim(), lastName.trim(), false))
        }
    }

    fun submitEmail(email: String) {
        if (email.isBlank()) {
            _authState.value = TelegramAuthState.Email("Введи адрес электронной почты.")
            return
        }
        runAuthAction(TelegramAuthState.Email(), "SetAuthenticationEmailAddress") {
            request(TdApi.SetAuthenticationEmailAddress(email.trim()))
        }
    }

    fun submitEmailCode(code: String) {
        if (code.isBlank()) {
            _authState.value = TelegramAuthState.EmailCode("Введи код из письма.")
            return
        }
        runAuthAction(TelegramAuthState.EmailCode(), "CheckAuthenticationEmailCode") {
            request(TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(code.trim())))
        }
    }

    fun openChat(chat: TelegramChat) {
        activeChatId = chat.id
        _messages.value = knownMessages[chat.id].orEmpty().toList()
        scope.launch {
            try {
                val result = request(TdApi.GetChatHistory(chat.id, 0, 0, 50, false))
                val mapped = result.messages.map(::mapMessage).sortedBy { it.timestamp }
                knownMessages[chat.id] = mapped.toMutableList()
                _messages.value = mapped
            } catch (error: Exception) {
                _notice.value = error.message ?: "Не удалось загрузить историю чата."
            }
        }
    }

    fun closeChat() {
        activeChatId = null
        _messages.value = emptyList()
    }

    fun sendMessage(chatId: Long, text: String) {
        if (text.isBlank()) return
        scope.launch {
            try {
                request(
                    TdApi.SendMessage(
                        chatId,
                        null,
                        null,
                        null,
                        null,
                        TdApi.InputMessageText(
                            TdApi.FormattedText(text.trim(), emptyArray()),
                            null,
                            true
                        )
                    )
                )
            } catch (error: Exception) {
                _notice.value = error.message ?: "Не удалось отправить сообщение."
            }
        }
    }

    fun addProxy(proxy: ProxyLink) {
        scope.launch {
            try {
                request(
                    TdApi.AddProxy(
                        TdApi.Proxy(proxy.server, proxy.port, TdApi.ProxyTypeMtproto(proxy.secret)),
                        true,
                        ""
                    )
                )
                _notice.value = "Прокси добавлен в TDLib и включён."
            } catch (error: Exception) {
                _notice.value = error.message ?: "Не удалось подключить прокси."
            }
        }
    }

    private fun initializeClient() {
        try {
            Client.execute(TdApi.SetLogVerbosityLevel(1))
            client = Client.create(
                { update ->
                    if (updates.trySend(update).isFailure) {
                        _notice.value = "Не удалось принять обновление Telegram."
                    }
                },
                { error -> _notice.value = "Ошибка TDLib: ${error.message ?: error.javaClass.simpleName}" },
                { error -> _notice.value = "Ошибка TDLib: ${error.message ?: error.javaClass.simpleName}" }
            )
        } catch (error: Exception) {
            _authState.value = TelegramAuthState.Failed(
                "Не удалось запустить TDLib: ${error.message ?: error.javaClass.simpleName}"
            )
        } catch (error: LinkageError) {
            _authState.value = TelegramAuthState.Failed(
                "Не удалось запустить TDLib: ${error.message ?: error.javaClass.simpleName}"
            )
        }
    }

    private suspend fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> handleAuthorizationState(update.authorizationState)
            is TdApi.UpdateConnectionState -> {
                _connectionState.value = when (update.state) {
                    is TdApi.ConnectionStateConnecting -> "Подключаемся"
                    is TdApi.ConnectionStateConnectingToProxy -> "Подключаемся через прокси"
                    is TdApi.ConnectionStateUpdating -> "Обновляем данные"
                    is TdApi.ConnectionStateReady -> "В сети"
                    else -> "Ожидаем сеть"
                }
            }
            is TdApi.UpdateNewChat -> {
                chatObjects[update.chat.id] = update.chat
                publishChats()
            }
            is TdApi.UpdateChatTitle -> {
                chatObjects[update.chatId]?.title = update.title
                publishChats()
            }
            is TdApi.UpdateChatLastMessage -> {
                chatObjects[update.chatId]?.lastMessage = update.lastMessage
                publishChats()
            }
            is TdApi.UpdateNewMessage -> {
                val mapped = mapMessage(update.message)
                val messages = knownMessages.getOrPut(mapped.chatId) { mutableListOf() }
                if (messages.none { it.id == mapped.id }) messages.add(mapped)
                if (activeChatId == mapped.chatId) _messages.value = messages.sortedBy { it.timestamp }
                chatObjects[mapped.chatId]?.lastMessage = update.message
                publishChats()
                if (!update.message.isOutgoing) cacheIncoming(arrayOf(update.message))
            }
            is TdApi.UpdateDeleteMessages -> {
                val deletedIds = update.messageIds.toSet()
                knownMessages[update.chatId]?.let { messages ->
                    val deletedAt = (System.currentTimeMillis() / 1000L).toInt()
                    val updated = messages.map { message ->
                        if (message.id in deletedIds) message.copy(deletedAt = deletedAt) else message
                    }
                    knownMessages[update.chatId] = updated.toMutableList()
                    if (activeChatId == update.chatId) _messages.value = updated
                }
                if (update.messageIds.isNotEmpty()) {
                    database.archiveDao().markDeleted(update.chatId, update.messageIds.toList())
                }
            }
            is TdApi.UpdateUser -> {
                if (update.user.id == me?.id) me = update.user
            }
        }
    }

    private suspend fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val dataDirectory = File(appContext.filesDir, "tdlib")
                val filesDirectory = File(dataDirectory, "files")
                check(dataDirectory.isDirectory || dataDirectory.mkdirs()) {
                    "Не удалось создать каталог базы данных Telegram."
                }
                check(filesDirectory.isDirectory || filesDirectory.mkdirs()) {
                    "Не удалось создать каталог файлов Telegram."
                }
                request(
                    TdApi.SetTdlibParameters(
                        false,
                        dataDirectory.absolutePath,
                        filesDirectory.absolutePath,
                        secureStorage.getOrCreateTelegramDatabaseKey().toByteArray(Charsets.UTF_8),
                        true,
                        true,
                        true,
                        true,
                        BuildConfig.TELEGRAM_API_ID,
                        BuildConfig.TELEGRAM_API_HASH,
                        appContext.resources.configuration.locales[0].toLanguageTag(),
                        Build.MODEL ?: "Android",
                        Build.VERSION.RELEASE ?: "Android",
                        BuildConfig.VERSION_NAME
                    )
                )
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> _authState.value = TelegramAuthState.Phone()
            is TdApi.AuthorizationStateWaitCode -> _authState.value = TelegramAuthState.Code()
            is TdApi.AuthorizationStateWaitPassword -> _authState.value = TelegramAuthState.Password(state.passwordHint)
            is TdApi.AuthorizationStateWaitRegistration -> _authState.value = TelegramAuthState.Registration()
            is TdApi.AuthorizationStateWaitEmailAddress -> _authState.value = TelegramAuthState.Email()
            is TdApi.AuthorizationStateWaitEmailCode -> _authState.value = TelegramAuthState.EmailCode()
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation ->
                _authState.value = TelegramAuthState.OtherDevice(state.link)
            is TdApi.AuthorizationStateReady -> {
                _authState.value = TelegramAuthState.Ready
                me = request(TdApi.GetMe())
                loadChats()
            }
            is TdApi.AuthorizationStateLoggingOut -> _authState.value = TelegramAuthState.Starting
            is TdApi.AuthorizationStateClosing -> _authState.value = TelegramAuthState.Starting
            is TdApi.AuthorizationStateClosed -> _authState.value =
                TelegramAuthState.Failed("Сессия Telegram закрыта. Перезапусти приложение.")
            else -> _authState.value =
                TelegramAuthState.Unsupported("TDLib запросила неподдерживаемый способ входа: ${state.javaClass.simpleName}")
        }
    }

    private suspend fun loadChats() {
        try {
            val result = request(TdApi.GetChats(TdApi.ChatListMain(), 100))
            result.chatIds.toList().map { chatId ->
                scope.async { request(TdApi.GetChat(chatId)) }
            }.awaitAll().forEach { chatObjects[it.id] = it }
            publishChats()
        } catch (error: Exception) {
            _notice.value = error.message ?: "Не удалось загрузить список чатов."
        }
    }

    private fun publishChats() {
        _chats.value = chatObjects.values
            .sortedByDescending { it.lastMessage?.date ?: 0 }
            .map { chat ->
                TelegramChat(
                    id = chat.id,
                    title = chat.title,
                    preview = chat.lastMessage?.let(::messagePreview).orEmpty(),
                    timestamp = chat.lastMessage?.date ?: 0,
                    unreadCount = chat.unreadCount,
                    isGroup = chat.type !is TdApi.ChatTypePrivate,
                    isDeveloper = (chat.type as? TdApi.ChatTypePrivate)?.userId == DEVELOPER_USER_ID
                )
            }
    }

    private fun mapMessage(message: TdApi.Message): TelegramMessage = TelegramMessage(
        id = message.id,
        chatId = message.chatId,
        sender = when (val sender = message.senderId) {
            is TdApi.MessageSenderUser -> when {
                sender.userId == me?.id -> "Ты"
                sender.userId == DEVELOPER_USER_ID -> "Разработчик"
                else -> "Контакт"
            }
            is TdApi.MessageSenderChat -> "Чат"
            else -> "Telegram"
        },
        text = messagePreview(message),
        timestamp = message.date,
        outgoing = message.isOutgoing,
        isDeveloper = (message.senderId as? TdApi.MessageSenderUser)?.userId == DEVELOPER_USER_ID
    )

    private fun messagePreview(message: TdApi.Message): String = when (val content = message.content) {
        is TdApi.MessageText -> content.text.text
        is TdApi.MessagePhoto -> content.caption.text.ifBlank { "Фото" }
        is TdApi.MessageVideo -> content.caption.text.ifBlank { "Видео" }
        is TdApi.MessageDocument -> content.caption.text.ifBlank { "Файл" }
        is TdApi.MessageVoiceNote -> "Голосовое сообщение"
        is TdApi.MessageSticker -> "Стикер"
        else -> "Сообщение"
    }

    private suspend fun cacheIncoming(messages: Array<TdApi.Message>) {
        if (!_archiveEnabled.value) return
        messages.filterNot { it.isOutgoing }.forEach { message ->
            database.archiveDao().save(
                ArchivedMessageEntity(
                    chatId = message.chatId,
                    messageId = message.id,
                    chatTitle = chatObjects[message.chatId]?.title.orEmpty(),
                    sender = mapMessage(message).sender,
                    encryptedText = secureStorage.encrypt(messagePreview(message)),
                    timestamp = message.date.toLong(),
                    deleted = false
                )
            )
        }
    }

    private fun runAuthAction(
        fallback: TelegramAuthState,
        actionName: String,
        action: suspend () -> TdApi.Object
    ) {
        scope.launch {
            try {
                action()
            } catch (error: Exception) {
                val message = error.message ?: "$actionName завершился ошибкой."
                _authState.value = withAuthError(fallback, message)
            }
        }
    }

    private fun withAuthError(state: TelegramAuthState, error: String): TelegramAuthState = when (state) {
        is TelegramAuthState.Phone -> state.copy(error = error)
        is TelegramAuthState.Code -> state.copy(error = error)
        is TelegramAuthState.Password -> state.copy(error = error)
        is TelegramAuthState.Registration -> state.copy(error = error)
        is TelegramAuthState.Email -> state.copy(error = error)
        is TelegramAuthState.EmailCode -> state.copy(error = error)
        else -> TelegramAuthState.Failed(error)
    }

    private suspend fun <T : TdApi.Object> request(function: TdApi.Function<T>): T {
        val activeClient = client ?: throw IllegalStateException("TDLib ещё не инициализирована.")
        val result = CompletableDeferred<T>()
        activeClient.send(
            function,
            { response ->
                if (response is TdApi.Error) {
                    result.completeExceptionally(IllegalStateException("${response.code}: ${response.message}"))
                } else {
                    @Suppress("UNCHECKED_CAST")
                    result.complete(response as T)
                }
            },
            { error -> result.completeExceptionally(error) }
        )
        return result.await()
    }

    companion object {
        private const val DEVELOPER_USER_ID = 6226314827L
        private const val PREFERENCES = "nicegram_preferences"
        private const val KEY_ARCHIVE_ENABLED = "archive_enabled"
    }
}
