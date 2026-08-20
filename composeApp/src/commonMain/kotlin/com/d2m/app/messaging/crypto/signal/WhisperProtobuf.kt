package com.d2m.app.messaging.crypto.signal

/**
 * Minimal, dependency-free Protocol Buffers reader/writer -- just enough to
 * encode/decode `WhisperMessage` and `PreKeyWhisperMessage` (varint +
 * length-delimited fields only; this schema has no nested/repeated/packed
 * fields, so a full protobuf runtime would be unnecessary weight). Field
 * numbers below are copied exactly from
 * `libsignal-protocol-protobuf-ts`'s `WhisperTextProtocol.proto`
 * (`java_package = "org.whispersystems.libsignal.protocol"`, the same
 * schema real Signal clients use) -- confirmed against that published
 * source directly, not approximated, since d2m_web's crypto library
 * (`@privacyresearch/libsignal-protocol-typescript`) encodes/decodes
 * exactly this wire format and mobile must match it byte-for-byte to
 * interoperate. See SignalProtocol.kt's doc comment for the full picture.
 *
 * ```proto
 * message WhisperMessage {
 *   optional bytes  ephemeralKey    = 1;
 *   optional uint32 counter         = 2;
 *   optional uint32 previousCounter = 3;
 *   optional bytes  ciphertext      = 4;
 * }
 * message PreKeyWhisperMessage {
 *   optional uint32 preKeyId       = 1;
 *   optional bytes  baseKey        = 2;
 *   optional bytes  identityKey    = 3;
 *   optional bytes  message        = 4;
 *   optional uint32 registrationId = 5;
 *   optional uint32 signedPreKeyId = 6;
 * }
 * ```
 */
internal object WhisperProto {
    private const val WM_EPHEMERAL_KEY = 1
    private const val WM_COUNTER = 2
    private const val WM_PREVIOUS_COUNTER = 3
    private const val WM_CIPHERTEXT = 4

    private const val PKWM_PRE_KEY_ID = 1
    private const val PKWM_BASE_KEY = 2
    private const val PKWM_IDENTITY_KEY = 3
    private const val PKWM_MESSAGE = 4
    private const val PKWM_REGISTRATION_ID = 5
    private const val PKWM_SIGNED_PRE_KEY_ID = 6

    fun encodeWhisperMessage(ephemeralKey: ByteArray, counter: Int, previousCounter: Int, ciphertext: ByteArray): ByteArray {
        val w = ProtoWriter()
        w.writeBytesField(WM_EPHEMERAL_KEY, ephemeralKey)
        w.writeVarintField(WM_COUNTER, counter.toUInt32Long())
        w.writeVarintField(WM_PREVIOUS_COUNTER, previousCounter.toUInt32Long())
        w.writeBytesField(WM_CIPHERTEXT, ciphertext)
        return w.toByteArray()
    }

    data class WhisperMessageFields(val ephemeralKey: ByteArray, val counter: Int, val previousCounter: Int, val ciphertext: ByteArray)

    fun decodeWhisperMessage(bytes: ByteArray): WhisperMessageFields {
        val r = ProtoReader(bytes)
        var ephemeralKey: ByteArray? = null
        var counter = 0
        var previousCounter = 0
        var ciphertext: ByteArray? = null
        while (r.hasNext()) {
            val (fieldNumber, wireType) = r.readTag()
            when (fieldNumber) {
                WM_EPHEMERAL_KEY -> ephemeralKey = r.readBytes()
                WM_COUNTER -> counter = r.readVarint().toInt()
                WM_PREVIOUS_COUNTER -> previousCounter = r.readVarint().toInt()
                WM_CIPHERTEXT -> ciphertext = r.readBytes()
                else -> r.skip(wireType)
            }
        }
        return WhisperMessageFields(
            ephemeralKey ?: error("WhisperMessage: missing ephemeralKey"),
            counter,
            previousCounter,
            ciphertext ?: error("WhisperMessage: missing ciphertext"),
        )
    }

    fun encodePreKeyWhisperMessage(registrationId: Int, preKeyId: Int?, signedPreKeyId: Int, baseKey: ByteArray, identityKey: ByteArray, message: ByteArray): ByteArray {
        val w = ProtoWriter()
        if (preKeyId != null) w.writeVarintField(PKWM_PRE_KEY_ID, preKeyId.toUInt32Long())
        w.writeBytesField(PKWM_BASE_KEY, baseKey)
        w.writeBytesField(PKWM_IDENTITY_KEY, identityKey)
        w.writeBytesField(PKWM_MESSAGE, message)
        w.writeVarintField(PKWM_REGISTRATION_ID, registrationId.toUInt32Long())
        w.writeVarintField(PKWM_SIGNED_PRE_KEY_ID, signedPreKeyId.toUInt32Long())
        return w.toByteArray()
    }

    data class PreKeyWhisperMessageFields(
        val registrationId: Int,
        val preKeyId: Int?,
        val signedPreKeyId: Int,
        val baseKey: ByteArray,
        val identityKey: ByteArray,
        val message: ByteArray,
    )

    fun decodePreKeyWhisperMessage(bytes: ByteArray): PreKeyWhisperMessageFields {
        val r = ProtoReader(bytes)
        var registrationId = 0
        var preKeyId: Int? = null
        var signedPreKeyId = 0
        var baseKey: ByteArray? = null
        var identityKey: ByteArray? = null
        var message: ByteArray? = null
        while (r.hasNext()) {
            val (fieldNumber, wireType) = r.readTag()
            when (fieldNumber) {
                PKWM_PRE_KEY_ID -> preKeyId = r.readVarint().toInt()
                PKWM_BASE_KEY -> baseKey = r.readBytes()
                PKWM_IDENTITY_KEY -> identityKey = r.readBytes()
                PKWM_MESSAGE -> message = r.readBytes()
                PKWM_REGISTRATION_ID -> registrationId = r.readVarint().toInt()
                PKWM_SIGNED_PRE_KEY_ID -> signedPreKeyId = r.readVarint().toInt()
                else -> r.skip(wireType)
            }
        }
        return PreKeyWhisperMessageFields(
            registrationId, preKeyId, signedPreKeyId,
            baseKey ?: error("PreKeyWhisperMessage: missing baseKey"),
            identityKey ?: error("PreKeyWhisperMessage: missing identityKey"),
            message ?: error("PreKeyWhisperMessage: missing message"),
        )
    }
}

private fun Int.toUInt32Long(): Long = this.toLong() and 0xFFFFFFFFL

private class ProtoWriter {
    private var buf = ByteArray(64)
    private var size = 0

    private fun ensure(extra: Int) {
        if (size + extra > buf.size) {
            var newSize = buf.size * 2
            while (newSize < size + extra) newSize *= 2
            buf = buf.copyOf(newSize)
        }
    }

    private fun writeByte(b: Int) {
        ensure(1)
        buf[size++] = b.toByte()
    }

    /** Standard protobuf base-128 varint: 7 payload bits per byte, continuation bit (0x80) set on every byte but the last. */
    fun writeVarint(value: Long) {
        var v = value
        while (true) {
            val low7 = (v and 0x7F).toInt()
            v = v ushr 7
            if (v == 0L) {
                writeByte(low7)
                return
            }
            writeByte(low7 or 0x80)
        }
    }

    private fun writeTag(fieldNumber: Int, wireType: Int) = writeVarint(((fieldNumber shl 3) or wireType).toLong())

    fun writeVarintField(fieldNumber: Int, value: Long) {
        writeTag(fieldNumber, 0)
        writeVarint(value)
    }

    fun writeBytesField(fieldNumber: Int, value: ByteArray) {
        writeTag(fieldNumber, 2)
        writeVarint(value.size.toLong())
        ensure(value.size)
        value.copyInto(buf, size)
        size += value.size
    }

    fun toByteArray(): ByteArray = buf.copyOf(size)
}

private class ProtoReader(private val bytes: ByteArray) {
    private var pos = 0

    fun hasNext(): Boolean = pos < bytes.size

    fun readTag(): Pair<Int, Int> {
        val tag = readVarint()
        return Pair((tag ushr 3).toInt(), (tag and 0x7L).toInt())
    }

    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val b = bytes[pos++].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result
    }

    fun readBytes(): ByteArray {
        val len = readVarint().toInt()
        val out = bytes.copyOfRange(pos, pos + len)
        pos += len
        return out
    }

    fun skip(wireType: Int) {
        when (wireType) {
            0 -> readVarint()
            2 -> readBytes()
            else -> error("WhisperProto: unsupported wire type $wireType")
        }
    }
}
