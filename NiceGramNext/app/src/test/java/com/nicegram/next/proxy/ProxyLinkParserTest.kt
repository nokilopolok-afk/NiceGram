package com.nicegram.next.proxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyLinkParserTest {
    @Test
    fun parsesTelegramProxyLink() {
        val result = ProxyLinkParser.parse("https://t.me/proxy?server=proxy.example&port=443&secret=abcdef")

        assertEquals(
            ProxyLinkParser.Result.Success(ProxyLink("proxy.example", 443, "abcdef")),
            result
        )
    }

    @Test
    fun rejectsUnsupportedHost() {
        val result = ProxyLinkParser.parse("https://example.com/proxy?server=proxy.example&port=443&secret=abc")

        assertTrue(result is ProxyLinkParser.Result.Error)
    }

    @Test
    fun rejectsInvalidPort() {
        val result = ProxyLinkParser.parse("https://t.me/proxy?server=proxy.example&port=70000&secret=abc")

        assertTrue(result is ProxyLinkParser.Result.Error)
    }
}
