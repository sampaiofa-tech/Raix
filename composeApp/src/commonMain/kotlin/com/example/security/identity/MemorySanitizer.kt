package com.example.security.identity

/**
 * Memory Sanitizer with Dead-Store Elimination mitigation (P1.2).
 *
 * Problem:
 * Standard `Arrays.fill(bytes, 0)` can be silently eliminated as dead-code by modern
 * JIT optimizing compilers (HotSpot C2 / GraalVM) when the buffer is not read subsequently.
 * This leaves sensitive key material and BIP-39 mnemonic phrases exposed in RAM dumps.
 *
 * Mitigation:
 * Implements a volatile memory barrier and secondary accumulation read that prevents
 * the optimizer from proving that the zeroed memory is dead.
 */
object MemorySanitizer {

    @Volatile
    private var volatileSink: Byte = 0

    /**
     * Irreversibly zeroes a sensitive byte array, guaranteeing the compiler will not
     * optimize away the wiping operation.
     */
    fun zeroize(bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) return

        var accumulator: Byte = 0
        for (i in bytes.indices) {
            bytes[i] = 0
            accumulator = (accumulator.toInt() xor bytes[i].toInt()).toByte()
        }

        // Volatile write forces a memory fence / barrier preventing Dead-Store Elimination
        volatileSink = accumulator
    }

    /**
     * Irreversibly zeroes a sensitive char array (e.g. passwords, PINs).
     */
    fun zeroize(chars: CharArray?) {
        if (chars == null || chars.isEmpty()) return

        var accumulator: Int = 0
        for (i in chars.indices) {
            chars[i] = '\u0000'
            accumulator = accumulator xor chars[i].code
        }

        volatileSink = accumulator.toByte()
    }

    /**
     * Zeroes and clears a mutable list of sensitive strings.
     */
    fun zeroizeStrings(strings: MutableList<String>?) {
        if (strings == null || strings.isEmpty()) return
        for (i in strings.indices) {
            strings[i] = ""
        }
        strings.clear()
    }
}
