package com.d2m.app.messaging.crypto.signal

/**
 * ============================================================================
 *  A from-spec X3DH + Double Ratchet implementation -- NEEDS REAL DEVICE
 *  VERIFICATION BEFORE IT CARRIES REAL USER CONVERSATIONS. READ BEFORE
 *  RELYING ON THIS.
 * ============================================================================
 *
 * Ported from the public-domain Signal specifications
 * (https://signal.org/docs/specifications/x3dh/,
 * .../doubleratchet/, .../xeddsa/ -- each explicitly placed in the public
 * domain by Signal, confirmed directly against the spec PDFs, not assumed),
 * with every wire-format and KDF detail (protobuf field numbers, HKDF info
 * strings, public-key prefix byte, IV derivation, MAC construction, session
 * bookkeeping) reverse-engineered from `@privacyresearch/libsignal-protocol-
 * typescript`'s actual TypeScript/compiled-JS source -- the library
 * d2m_web's crypto.ts wraps -- specifically so mobile can interoperate with
 * real web sessions, not just with itself. This was NOT approximated from
 * general "how Signal Protocol works" knowledge; every constant below
 * (info strings, byte layouts, DH ordering) was confirmed against that
 * library's source directly. Deliberately NOT using Signal's own official
 * `libsignal` (AGPLv3) or any other Signal-Protocol-compatible library --
 * none exists under a permissive license (confirmed via a dedicated license
 * search before starting this file) -- so this is original code written
 * from the public specifications.
 *
 * Despite that research rigor: this is genuinely one of the highest-stakes,
 * hardest-to-verify pieces of code in this codebase. It has NOT been run
 * against a real device, NOT been tested for actual interop with a live
 * d2m_web session, and has NOT had independent security review. A subtle
 * mistake here is silent -- it either fails to interoperate (annoying but
 * safe) or, worse, "looks encrypted" while being broken. Before this
 * carries real user conversations: verify actual mobile<->web message
 * exchange end-to-end on real devices, and get a second set of eyes on this
 * file specifically. One known, explicitly-flagged uncertainty: the exact
 * value libsignal-protocol-typescript places in the outgoing
 * PreKeyWhisperMessage's `registrationId` protobuf field (this
 * implementation uses the SENDER's own registrationId, matching standard
 * Signal Protocol convention/spec, but this specific line wasn't confirmed
 * byte-for-byte against that library's source during research -- worth
 * confirming first if cross-platform decryption fails specifically on the
 * very first message of a new conversation).
 *
 * Deliberate scope simplifications vs. the reference implementation (all
 * chosen for this app's actual usage pattern -- human-paced 1:1 chat, not
 * high-concurrency multi-device -- see SignalSessionStore.kt's doc comment
 * for the storage-side half of this):
 *  - One active session per peer, not a multi-generation archive of up to
 *    40 past sessions. `establishSession` always replaces (trust-on-first-
 *    use), matching d2m_web's own crypto.ts behavior exactly.
 *  - A bounded number of retired-but-still-decryptable receiving chains
 *    (SignalSessionStore's MAX_RETIRED_CHAINS), rather than the reference's
 *    `oldRatchetList` (confirmed, by direct source inspection, to have a
 *    latent bug that makes its 10-entry cap a no-op -- old chains are never
 *    actually evicted in the JS library as shipped).
 *
 * None of these simplifications affect the WIRE FORMAT or byte-for-byte
 * interop with web -- only local session-storage bookkeeping.
 */
internal object SignalProtocol {
    const val NUM_ONE_TIME_PREKEYS = 20
    private const val MAX_SKIP = 2000 // matches the reference implementation's own forward-fill safety limit

    // ---- Identity / prekey generation (mirrors crypto.ts's initIdentity/topUpPreKeys) ----

    fun generateIdentity(primitives: CryptoPrimitives): IdentityRecord {
        val registrationId = (primitives.randomBytes(2).let { ((it[0].toInt() and 0xFF) shl 8) or (it[1].toInt() and 0xFF) }) and 0x3FFF
        val (priv, pub) = primitives.x25519GenerateKeyPair()
        return IdentityRecord(registrationId, priv.toB64(), pub.toB64())
    }

    fun generateSignedPreKey(primitives: CryptoPrimitives, identity: IdentityRecord, keyId: Int): SignedPreKeyRecord {
        val (priv, pub) = primitives.x25519GenerateKeyPair()
        val signature = primitives.xEdDSASign(identity.privateKey.fromB64(), prefix05(pub))
        return SignedPreKeyRecord(keyId, priv.toB64(), pub.toB64(), signature.toB64())
    }

    fun generateOneTimePreKeys(primitives: CryptoPrimitives, startId: Int, count: Int): List<OneTimePreKeyRecord> =
        (0 until count).map { i ->
            val (priv, pub) = primitives.x25519GenerateKeyPair()
            OneTimePreKeyRecord(startId + i, priv.toB64(), pub.toB64())
        }

    fun buildKeyBundleUpload(identity: IdentityRecord, signedPreKey: SignedPreKeyRecord, oneTimePreKeys: List<OneTimePreKeyRecord>): KeyBundleUpload =
        KeyBundleUpload(
            registrationId = identity.registrationId,
            identityKey = prefix05(identity.publicKey.fromB64()).toB64(),
            signedPreKey = SignedPreKeyDto(signedPreKey.keyId, prefix05(signedPreKey.publicKey.fromB64()).toB64(), signedPreKey.signature),
            preKeys = oneTimePreKeys.map { PreKeyDto(it.keyId, prefix05(it.publicKey.fromB64()).toB64()) },
        )

    // ---- Session establishment: initiator (we fetched the peer's bundle) ----

    /**
     * X3DH as initiator, immediately followed by the reference
     * implementation's own extra Double-Ratchet "sending ratchet" step
     * (confirmed necessary via direct source trace -- the X3DH-derived
     * root key is NOT used directly as a chain key; a second, fresh
     * ephemeral keypair is generated and DH'd against the peer's signed
     * prekey to derive the actual first sending chain). See this file's
     * top doc comment for the exact source trace this was built from.
     */
    fun establishSessionAsInitiator(primitives: CryptoPrimitives, myIdentity: IdentityRecord, bundle: PreKeyBundleResponse): SessionRecord {
        val myIdentityPriv = myIdentity.privateKey.fromB64()
        val theirIdentityRaw = strip05(bundle.identityKey.fromB64())
        val theirSignedPreKeyRaw = strip05(bundle.signedPreKey.publicKey.fromB64())
        val theirOneTimePreKeyRaw = bundle.preKey?.let { strip05(it.publicKey.fromB64()) }

        val signatureOk = primitives.xEdDSAVerify(theirIdentityRaw, prefix05(theirSignedPreKeyRaw), bundle.signedPreKey.signature.fromB64())
        check(signatureOk) { "signed prekey signature verification failed for peer bundle" }

        // Fresh ephemeral for X3DH itself (EKa) -- distinct from the ratchet ephemeral below.
        val (eKaPriv, eKaPub) = primitives.x25519GenerateKeyPair()

        val dh1 = primitives.x25519Agree(myIdentityPriv, theirSignedPreKeyRaw)
        val dh2 = primitives.x25519Agree(eKaPriv, theirIdentityRaw)
        val dh3 = primitives.x25519Agree(eKaPriv, theirSignedPreKeyRaw)
        val dh4 = theirOneTimePreKeyRaw?.let { primitives.x25519Agree(eKaPriv, it) }
        val sharedSecret = X3DH_PREFIX_FF32 + dh1 + dh2 + dh3 + (dh4 ?: ByteArray(0))
        val (rootKey0, _, _) = whisperHkdf(primitives, sharedSecret, ZERO_32, INFO_X3DH)

        // Extra ratchet step: NEW ephemeral, DH against their signed prekey.
        val (sendingPriv, sendingPub) = primitives.x25519GenerateKeyPair()
        val ratchetShared = primitives.x25519Agree(sendingPriv, theirSignedPreKeyRaw)
        val (rootKey1, sendingChainKey, _) = whisperHkdf(primitives, ratchetShared, rootKey0, INFO_RATCHET)

        val session = SessionRecord(
            remoteIdentityKey = theirIdentityRaw.toB64(),
            remoteRegistrationId = bundle.registrationId,
            ratchet = RatchetRecord(
                rootKey = rootKey1.toB64(),
                ourPrivateKey = sendingPriv.toB64(),
                ourPublicKey = sendingPub.toB64(),
                lastRemotePublicKey = theirSignedPreKeyRaw.toB64(),
                previousCounter = 0,
            ),
            pendingPreKey = PendingPreKeyRecord(bundle.preKey?.keyId, bundle.signedPreKey.keyId, eKaPub.toB64()),
        )
        session.chains[sendingPub.toB64()] = ChainRecord(counter = -1, key = sendingChainKey.toB64(), type = "SENDING")
        return session
    }

    // ---- Session establishment: responder (peer's first message reached us) ----

    data class ResponderResult(val session: SessionRecord, val plaintext: ByteArray)

    /**
     * Handles an incoming PreKeyWhisperMessage: derives the same X3DH root
     * key the initiator derived (mirrored DH order, confirmed via source
     * trace), then immediately steps the ratchet using the embedded
     * WhisperMessage's ephemeral key (populating our receiving chain)
     * before decrypting -- exactly mirroring the reference implementation's
     * sequencing (it does NOT do its own extra sending-ratchet step here;
     * `maybeStepRatchet` derives both a receiving AND a fresh sending chain
     * in one pass, see that function).
     */
    fun handleIncomingPreKeyMessage(
        primitives: CryptoPrimitives,
        myIdentity: IdentityRecord,
        store: SignalSessionStore,
        wireBytes: ByteArray,
    ): ResponderResult {
        val fields = WhisperProto.decodePreKeyWhisperMessage(wireBytes.copyOfRange(1, wireBytes.size)) // strip leading version byte
        val theirIdentityRaw = strip05(fields.identityKey)
        val eKaRaw = strip05(fields.baseKey)

        // Look up which local signed prekey / one-time prekey THIS message actually references -- only knowable now that the message is decoded, not before.
        val signedPreKey = store.findSignedPreKey(fields.signedPreKeyId)
            ?: error("no local signed prekey for id ${fields.signedPreKeyId} -- was it rotated out before this message arrived?")
        val oneTimePreKey = fields.preKeyId?.let { store.consumeOneTimePreKey(it) } // single-use: consumed here so it's never reused

        val myIdentityPriv = myIdentity.privateKey.fromB64()
        val signedPreKeyPriv = signedPreKey.privateKey.fromB64()

        val dh1 = primitives.x25519Agree(signedPreKeyPriv, theirIdentityRaw)
        val dh2 = primitives.x25519Agree(myIdentityPriv, eKaRaw)
        val dh3 = primitives.x25519Agree(signedPreKeyPriv, eKaRaw)
        val dh4 = oneTimePreKey?.let { primitives.x25519Agree(it.privateKey.fromB64(), eKaRaw) }
        val sharedSecret = X3DH_PREFIX_FF32 + dh1 + dh2 + dh3 + (dh4 ?: ByteArray(0))
        val (rootKey0, _, _) = whisperHkdf(primitives, sharedSecret, ZERO_32, INFO_X3DH)

        val session = SessionRecord(
            remoteIdentityKey = theirIdentityRaw.toB64(),
            remoteRegistrationId = fields.registrationId,
            ratchet = RatchetRecord(
                rootKey = rootKey0.toB64(),
                // Placeholder until maybeStepRatchet below generates our real sending ephemeral -- mirrors the reference implementation, which reuses the signed-prekey keypair as a transient stand-in here.
                ourPrivateKey = signedPreKeyPriv.toB64(),
                ourPublicKey = signedPreKey.publicKey.fromB64().toB64(),
                lastRemotePublicKey = eKaRaw.toB64(),
                previousCounter = 0,
            ),
        )

        // Decode the embedded WhisperMessage to learn the actual ratchet ephemeral + counters, then step the ratchet BEFORE decrypting -- exact reference sequencing.
        // The embedded blob is itself [version(1)][protobuf][mac(8)] -- strip both ends to get just the protobuf for decoding.
        val inner = WhisperProto.decodeWhisperMessage(fields.message.copyOfRange(1, fields.message.size - 8))
        val remoteRatchetPub = strip05(inner.ephemeralKey)
        maybeStepRatchet(primitives, session, remoteRatchetPub, inner.previousCounter)

        val plaintext = decryptWhisperMessageInner(primitives, session, myIdentity.publicKey.fromB64(), fields.message, inner)
        return ResponderResult(session, plaintext)
    }

    // ---- The DH ratchet step (Double Ratchet's "diffie-hellman ratchet") ----

    /**
     * Fires when an incoming message's ratchet ephemeral key is one we
     * don't already have a chain for -- i.e. the peer has advanced to a new
     * ratchet epoch. Derives a new receiving chain against their new key,
     * AND eagerly derives our own next sending chain against a brand-new
     * ephemeral of ours (exact reference behavior -- see this file's top
     * doc comment for the source trace).
     */
    private fun maybeStepRatchet(primitives: CryptoPrimitives, session: SessionRecord, remoteEphemeralPub: ByteArray, incomingPreviousCounter: Int) {
        val remoteKeyB64 = remoteEphemeralPub.toB64()
        if (session.chains.containsKey(remoteKeyB64)) return // already have this epoch's chain

        val previousReceiving = session.chains[session.ratchet.lastRemotePublicKey]
        if (previousReceiving != null && previousReceiving.key != null) {
            fillMessageKeys(primitives, previousReceiving, incomingPreviousCounter)
            previousReceiving.key = null
            previousReceiving.retiredAtMillis = currentTimeMillisApprox()
        }

        val receivingShared = primitives.x25519Agree(session.ratchet.ourPrivateKey.fromB64(), remoteEphemeralPub)
        val (rootAfterReceiving, receivingChainKey, _) = whisperHkdf(primitives, receivingShared, session.ratchet.rootKey.fromB64(), INFO_RATCHET)
        session.chains[remoteKeyB64] = ChainRecord(counter = -1, key = receivingChainKey.toB64(), type = "RECEIVING")
        session.ratchet.rootKey = rootAfterReceiving.toB64()

        val previousSendingKey = session.ratchet.ourPublicKey
        session.chains[previousSendingKey]?.let { retiredSending ->
            session.ratchet.previousCounter = retiredSending.counter
            session.chains.remove(previousSendingKey)
        }

        val (newOurPriv, newOurPub) = primitives.x25519GenerateKeyPair()
        session.ratchet.ourPrivateKey = newOurPriv.toB64()
        session.ratchet.ourPublicKey = newOurPub.toB64()
        val sendingShared = primitives.x25519Agree(newOurPriv, remoteEphemeralPub)
        val (rootAfterSending, sendingChainKey, _) = whisperHkdf(primitives, sendingShared, session.ratchet.rootKey.fromB64(), INFO_RATCHET)
        session.chains[newOurPub.toB64()] = ChainRecord(counter = -1, key = sendingChainKey.toB64(), type = "SENDING")
        session.ratchet.rootKey = rootAfterSending.toB64()
        session.ratchet.lastRemotePublicKey = remoteKeyB64
    }

    private fun fillMessageKeys(primitives: CryptoPrimitives, chain: ChainRecord, targetCounter: Int) {
        val currentKey = chain.key ?: return
        check(targetCounter - chain.counter <= MAX_SKIP) { "over $MAX_SKIP messages into the future" }
        var key = currentKey.fromB64()
        while (chain.counter < targetCounter) {
            val (seed, next) = advanceChainKey(primitives, key)
            chain.messageKeys[(chain.counter + 1).toString()] = seed.toB64()
            key = next
            chain.counter += 1
        }
        chain.key = key.toB64()
    }

    // ---- Encrypt ----

    /** Returns (ciphertextType, wireBytes) -- ciphertextType is 3 ("prekey") while `session.pendingPreKey` is still set, else 1 ("signal"), exactly mirroring the reference implementation's `msg.type`. `myRegistrationId` is OUR OWN registration id (standard Signal Protocol convention: `PreKeyWhisperMessage.registrationId` identifies the message's SENDER, letting the recipient detect if the sender's identity was reinstalled -- see this file's top doc comment for why this specific field is flagged as not 100%-source-confirmed). */
    fun encrypt(primitives: CryptoPrimitives, session: SessionRecord, myIdentityPub: ByteArray, myRegistrationId: Int, plaintext: ByteArray): Pair<Int, ByteArray> {
        val sendingKeyB64 = session.ratchet.ourPublicKey
        val chain = session.chains[sendingKeyB64] ?: error("no sending chain -- session not established")
        fillMessageKeys(primitives, chain, chain.counter + 1)
        val counter = chain.counter
        val seed = chain.messageKeys.remove(counter.toString())?.fromB64() ?: error("missing message key seed for counter $counter")
        val (aesKey, macKey, iv) = deriveMessageKeys(primitives, seed)

        val ciphertext = primitives.aesCbcEncrypt(aesKey, iv, plaintext)
        val whisperBytes = WhisperProto.encodeWhisperMessage(prefix05(sendingKeyB64.fromB64()), counter, session.ratchet.previousCounter, ciphertext)
        val remoteIdentity = session.remoteIdentityKey.fromB64()
        val macInput = prefix05(myIdentityPub) + prefix05(remoteIdentity) + byteArrayOf(0x33) + whisperBytes
        val mac = primitives.hmacSha256(macKey, macInput).copyOf(8)
        val versionedWhisper = byteArrayOf(0x33) + whisperBytes + mac

        val pending = session.pendingPreKey
        return if (pending != null) {
            val preKeyBytes = WhisperProto.encodePreKeyWhisperMessage(
                registrationId = myRegistrationId,
                preKeyId = pending.preKeyId,
                signedPreKeyId = pending.signedPreKeyId,
                baseKey = prefix05(pending.baseKey.fromB64()),
                identityKey = prefix05(myIdentityPub),
                message = versionedWhisper,
            )
            3 to (byteArrayOf(0x33) + preKeyBytes)
        } else {
            1 to versionedWhisper
        }
    }

    // ---- Decrypt (ordinary "signal"/type-1 WhisperMessage on an existing session) ----

    fun decryptWhisperMessage(primitives: CryptoPrimitives, session: SessionRecord, myIdentityPub: ByteArray, wireBytes: ByteArray): ByteArray {
        // [version(1)][protobuf][mac(8)] -- strip both ends to get just the protobuf for decoding.
        val inner = WhisperProto.decodeWhisperMessage(wireBytes.copyOfRange(1, wireBytes.size - 8))
        val remoteRatchetPub = strip05(inner.ephemeralKey)
        if (!session.chains.containsKey(remoteRatchetPub.toB64())) {
            maybeStepRatchet(primitives, session, remoteRatchetPub, inner.previousCounter)
        }
        return decryptWhisperMessageInner(primitives, session, myIdentityPub, wireBytes, inner)
    }

    private fun decryptWhisperMessageInner(
        primitives: CryptoPrimitives,
        session: SessionRecord,
        myIdentityPub: ByteArray,
        wireBytes: ByteArray,
        inner: WhisperProto.WhisperMessageFields,
    ): ByteArray {
        val remoteRatchetPub = strip05(inner.ephemeralKey)
        val chain = session.chains[remoteRatchetPub.toB64()] ?: error("no receiving chain for this message's ratchet key")

        val macInput = prefix05(session.remoteIdentityKey.fromB64()) + prefix05(myIdentityPub) + byteArrayOf(0x33) + wireBytes.copyOfRange(1, wireBytes.size - 8)
        val macKeySeed = messageKeySeedFor(primitives, chain, inner.counter)
        val (aesKey, macKey, iv) = deriveMessageKeys(primitives, macKeySeed)
        val expectedMac = primitives.hmacSha256(macKey, macInput).copyOf(8)
        val actualMac = wireBytes.copyOfRange(wireBytes.size - 8, wireBytes.size)
        check(expectedMac.contentEquals(actualMac)) { "MAC verification failed -- ciphertext rejected" }

        val plaintext = primitives.aesCbcDecrypt(aesKey, iv, inner.ciphertext)
        session.pendingPreKey = null // any successful decrypt proves the peer has this session; stop wrapping our own outgoing messages in PreKeyWhisperMessage.
        return plaintext
    }

    private fun messageKeySeedFor(primitives: CryptoPrimitives, chain: ChainRecord, counter: Int): ByteArray {
        chain.messageKeys[counter.toString()]?.let { return it.fromB64() }
        fillMessageKeys(primitives, chain, counter)
        return chain.messageKeys.remove(counter.toString())?.fromB64() ?: error("missing message key for counter $counter after fill")
    }
}

/** No kotlinx-datetime dependency needed just for a bookkeeping timestamp used only to order eviction -- an incrementing counter serves the same purpose and stays commonMain-simple. */
private var evictionClock = 0L
private fun currentTimeMillisApprox(): Long = ++evictionClock
