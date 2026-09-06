package com.example.security

import com.example.security.identity.MemorySanitizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemorySanitizerTest {

    @Test
    fun testZeroizeByteArrayWipesAllBytes() {
        val sensitiveBuffer = byteArrayOf(0x41, 0x42, 0x43, 0x7F, -1, 0x12)
        MemorySanitizer.zeroize(sensitiveBuffer)

        assertTrue(sensitiveBuffer.all { it == 0.toByte() }, "Todos os bytes devem ser reduzidos a zero")
    }

    @Test
    fun testZeroizeCharArrayWipesAllChars() {
        val sensitivePin = charArrayOf('1', '2', '3', '4', '5', '6')
        MemorySanitizer.zeroize(sensitivePin)

        assertTrue(sensitivePin.all { it == '\u0000' }, "Todos os caracteres devem ser reduzidos a nulo")
    }

    @Test
    fun testZeroizeStringsClearsList() {
        val sensitiveWords = mutableListOf("palavra1", "palavra2", "palavra3")
        MemorySanitizer.zeroizeStrings(sensitiveWords)

        assertEquals(0, sensitiveWords.size, "A lista deve ser completamente limpa")
    }
}
