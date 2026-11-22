package com.nicegram.next.proxy

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ProxyLink(
    val server: String,
    val port: Int,
    val secret: String
)

object ProxyLinkParser {
    sealed interface Result {
        data class Success(val proxy: ProxyLink) : Result
        data class Error(val message: String) : Result
    }

    fun parse(link: String): Result {
        val uri = try {
            URI(link.trim())
        } catch (_: URISyntaxException) {
            return Result.Error("Проверь формат ссылки.")
        }

        if (uri.scheme !in setOf("http", "https") ||
            uri.host !in setOf("t.me", "www.t.me", "telegram.me", "www.telegram.me") ||
            uri.path != "/proxy"
        ) {
            return Result.Error("Поддерживаются только ссылки t.me/proxy.")
        }

        val parameters = try {
            uri.rawQuery.orEmpty()
                .split('&')
                .filter(String::isNotEmpty)
                .associate { item ->
                    val separator = item.indexOf('=')
                    val key = decode(if (separator < 0) item else item.substring(0, separator))
                    val value = decode(if (separator < 0) "" else item.substring(separator + 1))
                    key to value
                }
        } catch (_: IllegalArgumentException) {
            return Result.Error("В ссылке есть некорректное кодирование.")
        }

        val server = parameters["server"]?.trim().orEmpty()
        if (server.isBlank() || server.any { it.isWhitespace() }) {
            return Result.Error("В ссылке не указан корректный сервер.")
        }

        val port = parameters["port"]?.toIntOrNull()
        if (port == null || port !in 1..65535) {
            return Result.Error("Укажи порт от 1 до 65535.")
        }

        val secret = parameters["secret"].orEmpty()
        if (secret.isBlank() || secret.any { it.isWhitespace() }) {
            return Result.Error("В ссылке отсутствует секрет прокси.")
        }

        return Result.Success(ProxyLink(server, port, secret))
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8)
}
