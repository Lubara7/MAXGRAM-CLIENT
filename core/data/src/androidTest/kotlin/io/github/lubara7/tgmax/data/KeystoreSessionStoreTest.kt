package io.github.lubara7.tgmax.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.lubara7.tgmax.domain.Session
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreSessionStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = KeystoreSessionStore(context)
    private val file get() = File(context.noBackupFilesDir, "max-session.bin")

    @Before fun before() = runBlocking { store.clear() }
    @After fun after() = runBlocking { store.clear() }

    @Test fun ciphertextRoundTripAndClear() = runBlocking {
        val secret = "test-credential-that-must-not-be-plaintext"
        store.write(Session("test-user", secret))
        assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains(secret))
        assertEquals(secret, store.read()?.credential)
        store.clear()
        assertNull(store.read())
    }

    @Test fun tamperedCiphertextIsRejected() = runBlocking {
        store.write(Session("test-user", "secret"))
        val bytes = file.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        file.writeBytes(bytes)
        try {
            store.read()
            fail("Tampered ciphertext was accepted")
        } catch (_: java.security.GeneralSecurityException) { /* expected */ }
    }
}
