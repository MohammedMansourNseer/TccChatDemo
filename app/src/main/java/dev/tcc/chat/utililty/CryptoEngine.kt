package dev.tcc.chat.utililty

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Singleton
class CryptoEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {
        private const val KEY_ALIAS = "ChatEncryptionKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_SIZE_BITS = 256

        private const val PREFS_NAME = "crypto_prefs"
        private const val DATA_KEY_CT = "data_key_ct"
        private const val DATA_KEY_IV = "data_key_iv"
        private const val DATA_KEY_LEN = 32

        private const val IV_SIZE_BYTES = 12

        private val secureRandomTL = ThreadLocal.withInitial { SecureRandom() }
        private val dataEncCipherTL = ThreadLocal<Cipher>()
        private val dataDecCipherTL = ThreadLocal<Cipher>()
        private val wrapEncCipherTL = ThreadLocal<Cipher>()
        private val wrapDecCipherTL = ThreadLocal<Cipher>()
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private val keystoreKey: SecretKey by lazy {
        createKeyIfNotExists()
        keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private val dataKey: SecretKey by lazy { loadOrCreateDataKey() }

    fun encrypt(plaintext: String): EncryptedData {
        val (cipher, iv) = obtainEncCipherForDataKey()
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedData(ciphertext = ciphertext, iv = iv)
    }

    fun decrypt(encryptedData: EncryptedData): String {
        val cipher = obtainDecCipherForDataKey(encryptedData.iv)
        val pt = cipher.doFinal(encryptedData.ciphertext)
        return String(pt, Charsets.UTF_8)
    }

    suspend fun encryptToBase64(plain: String): Pair<String, String> = withContext(Dispatchers.Default) {
        val (cipher, iv) = obtainEncCipherForDataKey()
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(ct, Base64.NO_WRAP) to Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    suspend fun decryptFromBase64(ctBase64: String, ivBase64: String): String = withContext(Dispatchers.Default) {
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipher = obtainDecCipherForDataKey(iv)
        val ct = Base64.decode(ctBase64, Base64.NO_WRAP)
        val pt = cipher.doFinal(ct)
        String(pt, Charsets.UTF_8)
    }

    fun getRawKeyBytes(): ByteArray = dataKey.encoded

    private fun createKeyIfNotExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
            gen.init(spec)
            gen.generateKey()
        }
    }

    private fun loadOrCreateDataKey(): SecretKey {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ctB64 = prefs.getString(DATA_KEY_CT, null)
        val ivB64 = prefs.getString(DATA_KEY_IV, null)

        if (ctB64 != null && ivB64 != null) {
            val ct = Base64.decode(ctB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val raw = decryptWithKeystore(ct, iv)
            return SecretKeySpec(raw, "AES")
        }

        val raw = ByteArray(DATA_KEY_LEN).also { secureRandomTL.get().nextBytes(it) }
        val (wrapCt, wrapIv) = encryptWithKeystore(raw)

        prefs.edit()
            .putString(DATA_KEY_CT, Base64.encodeToString(wrapCt, Base64.NO_WRAP))
            .putString(DATA_KEY_IV, Base64.encodeToString(wrapIv, Base64.NO_WRAP))
            .apply()

        return SecretKeySpec(raw, "AES")
    }

    private fun encryptWithKeystore(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = wrapEncCipherTL.get() ?: Cipher.getInstance(TRANSFORMATION).also {
            wrapEncCipherTL.set(it)
        }
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)
        val iv = cipher.iv
        val ct = cipher.doFinal(plain)
        return ct to iv
    }

    private fun decryptWithKeystore(ct: ByteArray, iv: ByteArray): ByteArray {
        val cipher = wrapDecCipherTL.get() ?: Cipher.getInstance(TRANSFORMATION).also {
            wrapDecCipherTL.set(it)
        }
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ct)
    }

    private fun obtainEncCipherForDataKey(): Pair<Cipher, ByteArray> {
        val cipher = dataEncCipherTL.get() ?: Cipher.getInstance(TRANSFORMATION).also {
            dataEncCipherTL.set(it)
        }
        val iv = ByteArray(IV_SIZE_BYTES).also { secureRandomTL.get().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, dataKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher to iv
    }

    private fun obtainDecCipherForDataKey(iv: ByteArray): Cipher {
        val cipher = dataDecCipherTL.get() ?: Cipher.getInstance(TRANSFORMATION).also {
            dataDecCipherTL.set(it)
        }
        cipher.init(Cipher.DECRYPT_MODE, dataKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher
    }

    data class EncryptedData(
        val ciphertext: ByteArray,
        val iv: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncryptedData
            if (!ciphertext.contentEquals(other.ciphertext)) return false
            if (!iv.contentEquals(other.iv)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }
}
