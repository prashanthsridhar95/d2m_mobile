package com.d2m.app.messaging.crypto.signal

import kotlinx.serialization.Serializable

/**
 * Mirrors messaging-framework/packages/protocol/src/index.ts's KeyBundleUpload/
 * PreKeyBundleResponse exactly (field-for-field, same camelCase names) --
 * these are the wire types d2m_web's api.ts already POSTs/GETs against the
 * relay server's existing `/keys/:username[...]` endpoints (confirmed via
 * direct research against both the web client and the server source; no
 * server-side changes were needed for this). All public-key/signature
 * fields are base64 of the RAW bytes described in SignalCodec.kt's doc
 * comment (33-byte 0x05-prefixed public keys, 64-byte raw XEdDSA
 * signatures) -- this file only carries the JSON shape, not the byte
 * conventions inside each field.
 */
@Serializable
data class SignedPreKeyDto(val keyId: Int, val publicKey: String, val signature: String)

@Serializable
data class PreKeyDto(val keyId: Int, val publicKey: String)

@Serializable
data class KeyBundleUpload(
    val registrationId: Int,
    val identityKey: String,
    val signedPreKey: SignedPreKeyDto,
    val preKeys: List<PreKeyDto>,
)

@Serializable
data class PreKeyBundleResponse(
    val registrationId: Int,
    val identityKey: String,
    val signedPreKey: SignedPreKeyDto,
    val preKey: PreKeyDto? = null,
)

@Serializable
data class KeyCountResponse(val oneTimePreKeys: Int)
