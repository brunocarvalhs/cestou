package br.com.brunocarvalhs.howmuch.feature.auth.domain.wear

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Key agreement behind the pairing code (sdd-055, §5.1). Both devices run the same derivation
 * over the same two public keys, so both show the same 8 letters without anything typed.
 *
 * Only standard JCE primitives: EC P-256 for ECDH, HKDF-SHA256 for the code. Keys and the shared
 * secret live in memory for one session and are never logged or persisted.
 *
 * Pure JVM code (no Android types): covered by plain unit tests.
 */
internal object PairingHandshake {

    private const val CURVE = "secp256r1"
    private const val HASH = "HmacSHA256"
    private const val HASH_LENGTH = 32
    private const val INFO_PREFIX = "cestou-wear-pairing-sas-v1"
    private const val SESSION_ID_BYTES = 16
    private const val CODE_LETTERS = 8

    /** Letters per half of the displayed code: `XXXX XXXX`. */
    private const val CODE_GROUP = 4
    private const val BYTE_MASK = 0xFF

    /** 21 consonants: no vowels, so the code cannot spell common words. */
    internal const val ALPHABET = "BCDFGHJKLMNPQRSTVWXYZ"

    /** Largest multiple of the alphabet size that fits in a byte (21 * 12). Bytes above it are rejected. */
    private const val REJECT_FROM = 252

    /** 64 bytes of output: enough that rejection sampling never runs out of bytes for 8 letters. */
    private const val OUTPUT_LENGTH = 64

    private val random = SecureRandom()
    private val base64Url = Base64.getUrlEncoder().withoutPadding()
    private val base64UrlDecoder = Base64.getUrlDecoder()

    /** Random session id, base64url. It travels in the deeplink and in every message of the session. */
    fun newSessionId(): String {
        val bytes = ByteArray(SESSION_ID_BYTES)
        random.nextBytes(bytes)
        return base64Url.encodeToString(bytes)
    }

    fun newKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").run {
        initialize(ECGenParameterSpec(CURVE), random)
        generateKeyPair()
    }

    /** X.509 encoding of the public key, the form that goes over the wire (base64url). */
    fun encodePublicKey(keyPair: KeyPair): String = base64Url.encodeToString(keyPair.public.encoded)

    /** Returns the raw X.509 bytes for a base64url key, or null when it is not a valid EC P-256 key. */
    fun decodePublicKey(encoded: String): ByteArray? = try {
        val bytes = base64UrlDecoder.decode(encoded)
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
        bytes
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: java.security.GeneralSecurityException) {
        null
    }

    /** ECDH between [privateKey] and the peer's X.509 public key. */
    fun sharedSecret(privateKey: PrivateKey, peerPublicKey: ByteArray): ByteArray {
        val peer = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(peerPublicKey))
        return KeyAgreement.getInstance("ECDH").run {
            init(privateKey)
            doPhase(peer, true)
            generateSecret()
        }
    }

    /**
     * Derives the pairing code. [wearPublicKey] and [phonePublicKey] are the raw X.509 bytes,
     * always in this order on both devices. Returns `XXXX XXXX`.
     */
    fun deriveCode(sharedSecret: ByteArray, wearPublicKey: ByteArray, phonePublicKey: ByteArray): String {
        val transcript = wearPublicKey + phonePublicKey
        val prk = hmac(ByteArray(HASH_LENGTH), sharedSecret)
        val okm = hkdfExpand(prk, INFO_PREFIX.toByteArray() + transcript, OUTPUT_LENGTH)
        return formatCode(lettersFrom(okm))
    }

    /**
     * Maps bytes to letters without modulo bias: bytes at or above [REJECT_FROM] are skipped.
     * Throws only if [okm] has fewer usable bytes than needed, which the output length makes
     * practically impossible.
     */
    internal fun lettersFrom(okm: ByteArray): String {
        val letters = okm.asSequence()
            .map { it.toInt() and BYTE_MASK }
            .filter { it < REJECT_FROM }
            .map { ALPHABET[it % ALPHABET.length] }
            .take(CODE_LETTERS)
            .joinToString(separator = "")
        check(letters.length == CODE_LETTERS) { "Not enough usable bytes for the pairing code" }
        return letters
    }

    internal fun formatCode(letters: String): String =
        letters.substring(0, CODE_GROUP) + " " + letters.substring(CODE_GROUP)

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val output = ByteArray(length)
        var previous = ByteArray(0)
        var written = 0
        var counter = 1
        while (written < length) {
            previous = hmac(prk, previous + info + byteArrayOf(counter.toByte()))
            val take = minOf(previous.size, length - written)
            previous.copyInto(output, destinationOffset = written, endIndex = take)
            written += take
            counter++
        }
        return output
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HASH)
        mac.init(SecretKeySpec(key, HASH))
        return mac.doFinal(data)
    }
}
