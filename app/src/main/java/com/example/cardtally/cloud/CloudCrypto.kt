package com.example.cardtally.cloud

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object CloudCrypto {
    fun key(password: CharArray, salt: ByteArray): ByteArray {
        require(password.size >= 8 && salt.size == 16)
        val spec = PBEKeySpec(password, salt, 200_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
    fun hmac(key: ByteArray, bytes: ByteArray): String = hex(Mac.getInstance("HmacSHA256").apply {
        init(SecretKeySpec(key, "HmacSHA256"))
    }.doFinal(bytes))
    fun id(key: ByteArray, file: File): String {
        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
        file.inputStream().use { input -> val buffer = ByteArray(32768); while (true) {
            val count = input.read(buffer); if (count < 0) break; mac.update(buffer, 0, count)
        } }
        return hex(mac.doFinal())
    }
    fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(32768); while (true) {
            val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count)
        } }
        return hex(digest.digest())
    }
    fun encrypt(key: ByteArray, source: File, target: File) {
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            updateAAD("CardTallyCloud1".toByteArray())
        }
        target.outputStream().use { out -> out.write(nonce)
            CipherOutputStream(out, cipher).use { encrypted -> source.inputStream().use { it.copyTo(encrypted) } }
        }
    }
    fun decrypt(key: ByteArray, source: File, target: File, limit: Long) {
        require(source.length() in 28..(limit + 28))
        source.inputStream().use { input ->
            val nonce = ByteArray(12); require(input.read(nonce) == nonce.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                updateAAD("CardTallyCloud1".toByteArray())
            }
            target.outputStream().use { out ->
                val buffer = ByteArray(32768); var total = 0L
                fun write(bytes: ByteArray?) { if (bytes != null) {
                    total += bytes.size; require(total <= limit); out.write(bytes)
                } }
                while (true) {
                    if (Thread.currentThread().isInterrupted) throw InterruptedException()
                    val count = input.read(buffer); if (count < 0) break
                    write(cipher.update(buffer, 0, count))
                }
                // Explicit doFinal is essential: stream wrappers may suppress authentication failures.
                write(cipher.doFinal())
                out.fd.sync()
            }
        }
    }
}
