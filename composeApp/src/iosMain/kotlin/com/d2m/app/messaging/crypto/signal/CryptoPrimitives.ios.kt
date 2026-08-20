package com.d2m.app.messaging.crypto.signal

/**
 * Not implemented -- same "real Xcode project + native crypto binding
 * needed first" situation as `WebRtcEngine.ios.kt` (see that file's doc
 * comment). Every method throws so a mistaken call fails loudly rather
 * than silently producing wrong bytes. In practice this is never actually
 * invoked: `createCryptoProvider.kt`'s iOS actual binds
 * `StubUnencryptedCryptoProvider` directly rather than constructing a
 * `SignalCryptoProvider` around this, so nothing on iOS ever reaches these
 * methods today. This actual exists only because `expect fun
 * createCryptoPrimitives()` needs one on every compiled target.
 */
actual fun createCryptoPrimitives(): CryptoPrimitives = object : CryptoPrimitives {
    private fun notImplemented(): Nothing = throw NotImplementedError(
        "Real Signal Protocol crypto isn't implemented on iOS yet -- needs a real Xcode project first (see iosApp/README.md), same as WebRtcEngine.ios.kt. iOS uses StubUnencryptedCryptoProvider today (see createCryptoProvider.kt), so this should never actually be called.",
    )

    override fun randomBytes(length: Int): ByteArray = notImplemented()
    override fun x25519GenerateKeyPair(): Pair<ByteArray, ByteArray> = notImplemented()
    override fun x25519Agree(privateKey: ByteArray, publicKey: ByteArray): ByteArray = notImplemented()
    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray = notImplemented()
    override fun aesCbcEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray = notImplemented()
    override fun aesCbcDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray = notImplemented()
    override fun xEdDSASign(privateKey: ByteArray, message: ByteArray): ByteArray = notImplemented()
    override fun xEdDSAVerify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean = notImplemented()
}
