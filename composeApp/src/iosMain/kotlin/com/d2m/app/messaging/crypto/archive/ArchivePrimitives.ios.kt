package com.d2m.app.messaging.crypto.archive

/**
 * Not implemented -- same situation as `CryptoPrimitives.ios.kt` (see that
 * file's doc comment): every method throws so a mistaken call fails loudly
 * rather than silently producing wrong bytes. `ArchiveManager.checkAfterConnect`/
 * `buildArchiveField` never reach these (they only touch a device's OWN
 * keypair, which stays null on iOS with nothing local cached), so the
 * setup/restore PIN prompt simply never gets a chance to appear in
 * practice on iOS today -- messaging itself is still the non-production
 * `StubUnencryptedCryptoProvider` there (see createCryptoProvider.kt), so
 * there is no real Signal identity for an archive to attach to yet either.
 * If `submitSetupPin`/`submitRestorePin` WERE invoked on iOS regardless,
 * they'd surface this as a caught, user-visible `archiveError` (both are
 * wrapped in try/catch) rather than a crash. This actual exists only
 * because `expect fun createArchivePrimitives()` needs one on every
 * compiled target.
 */
actual fun createArchivePrimitives(): ArchivePrimitives = object : ArchivePrimitives {
    private fun notImplemented(): Nothing = throw NotImplementedError(
        "Archive Keypair crypto isn't implemented on iOS yet -- needs a real Xcode project first (see iosApp/README.md), same as WebRtcEngine.ios.kt / CryptoPrimitives.ios.kt.",
    )

    override fun randomBytes(length: Int): ByteArray = notImplemented()
    override fun ecP256GenerateKeyPair(): EcKeyPair = notImplemented()
    override fun ecdhSharedSecretAsAesKey(privateKeyPkcs8: ByteArray, publicKeyRawPoint: ByteArray): ByteArray = notImplemented()
    override fun ecPrivateKeyScalar(privateKeyPkcs8: ByteArray): ByteArray = notImplemented()
    override fun ecPrivateKeyFromScalar(d: ByteArray): ByteArray = notImplemented()
    override fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray = notImplemented()
    override fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray = notImplemented()
    override fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray = notImplemented()
}
