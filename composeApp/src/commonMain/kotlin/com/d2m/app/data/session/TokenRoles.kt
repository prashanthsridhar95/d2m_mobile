package com.d2m.app.data.session

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Reads the `roles` claim from a JWT payload WITHOUT verifying the signature.
 * UI gating only: the server's require_staff dependency is the real enforcement.
 */
@OptIn(ExperimentalEncodingApi::class)
fun tokenRoles(token: String?): Set<String> {
    val payload = token?.split('.')?.getOrNull(1) ?: return emptySet()
    return runCatching {
        val bytes = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload)
        val obj: JsonObject = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
        when (val r = obj["roles"]) {
            is JsonArray -> r.mapNotNull { (it as? JsonPrimitive)?.content }.toSet()
            is JsonPrimitive -> setOf(r.content)
            else -> emptySet()
        }
    }.getOrDefault(emptySet())
}

fun Identity.isStaff(): Boolean = "staff" in tokenRoles(wedlockAccessToken)
