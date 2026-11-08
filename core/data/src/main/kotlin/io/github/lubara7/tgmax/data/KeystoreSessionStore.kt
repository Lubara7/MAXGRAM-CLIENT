package io.github.lubara7.tgmax.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import io.github.lubara7.tgmax.domain.Session
import io.github.lubara7.tgmax.domain.SessionStore
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Only ciphertext is persisted, under noBackupFilesDir. OTP values never reach this store. */
class KeystoreSessionStore(context: Context) : SessionStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "max-session.bin"))
    private val alias = "${context.packageName}.session.v1"
    private val mutex = Mutex()

    override suspend fun read(): Session? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bytes = try { file.openRead().use { stream ->
                val buffer = ByteArray(MAX_BYTES + 1)
                var count = 0
                while (count < buffer.size) {
                    val read = stream.read(buffer, count, buffer.size - count)
                    if (read < 0) break
                    count += read
                }
                buffer.copyOf(count)
            } }
            catch (_: java.io.FileNotFoundException) { return@withLock null }
            if (bytes.size > MAX_BYTES) throw IOException("Invalid session size")
            val input = DataInputStream(bytes.inputStream())
            if (input.readUnsignedByte() != 1) throw IOException("Unknown session version")
            val ivLength = input.readUnsignedByte()
            if (ivLength != 12) throw IOException("Invalid nonce size")
            val iv = ByteArray(ivLength).also(input::readFully)
            val encrypted = input.readBytes()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val json = JSONObject(cipher.doFinal(encrypted).toString(Charsets.UTF_8))
            Session(json.getString("userId"), json.getString("credential"))
        }
    }

    override suspend fun write(session: Session): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val json = JSONObject().put("userId", session.userId).put("credential", session.credential)
            val plain = json.toString().toByteArray(Charsets.UTF_8)
            if (plain.size > MAX_BYTES - 64) throw IOException("Session is too large")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.doFinal(plain)
            val record = ByteArrayOutputStream().also { buffer ->
                DataOutputStream(buffer).use {
                    it.writeByte(1)
                    it.writeByte(cipher.iv.size)
                    it.write(cipher.iv)
                    it.write(encrypted)
                }
            }.toByteArray()
            val output = file.startWrite()
            try {
                output.write(record)
                file.finishWrite(output)
            } catch (e: Exception) {
                file.failWrite(output)
                throw e
            }
        }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock { file.delete() }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    private companion object { const val MAX_BYTES = 65_536 }
}
