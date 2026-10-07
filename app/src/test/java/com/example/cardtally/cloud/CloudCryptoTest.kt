package com.example.cardtally.cloud

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CloudCryptoTest {
    @Test fun encryptedCopiesShareContentIdButNotCiphertext() {
        val folder = java.nio.file.Files.createTempDirectory("cloud-crypto").toFile()
        val key = ByteArray(32) { it.toByte() }
        try {
            val source = File(folder, "source").apply { writeText("record photo bytes".repeat(200)) }
            val first = File(folder, "first"); val second = File(folder, "second"); val restored = File(folder, "restored")
            CloudCrypto.encrypt(key, source, first); CloudCrypto.encrypt(key, source, second)
            assertNotEquals(CloudCrypto.hash(first), CloudCrypto.hash(second))
            CloudCrypto.decrypt(key, first, restored, 10000)
            assertArrayEquals(source.readBytes(), restored.readBytes())
            assertEquals(CloudCrypto.id(key, source), CloudCrypto.id(key, restored))
            assertNotEquals(CloudCrypto.id(key, source), CloudCrypto.id(ByteArray(32) { 42 }, source))
        } finally { folder.deleteRecursively(); key.fill(0) }
    }
    @Test fun tamperingWrongKeysAndOversizedFilesAreRejected() {
        val folder = java.nio.file.Files.createTempDirectory("cloud-integrity").toFile()
        try {
            val source = File(folder, "source").apply { writeText("private data") }
            val encrypted = File(folder, "encrypted"); val output = File(folder, "output")
            CloudCrypto.encrypt(ByteArray(32) { 1 }, source, encrypted)
            fun rejected(block: () -> Unit) {
                var failed = false; try { block() } catch (_: Exception) { failed = true }; assertTrue(failed)
            }
            rejected { CloudCrypto.decrypt(ByteArray(32) { 2 }, encrypted, output, 100) }
            rejected { CloudCrypto.decrypt(ByteArray(32) { 1 }, encrypted, output, 2) }
            val bytes = encrypted.readBytes(); bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte(); encrypted.writeBytes(bytes)
            rejected { CloudCrypto.decrypt(ByteArray(32) { 1 }, encrypted, output, 100) }
        } finally { folder.deleteRecursively() }
    }
}
