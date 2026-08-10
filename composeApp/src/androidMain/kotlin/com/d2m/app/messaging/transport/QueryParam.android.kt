package com.d2m.app.messaging.transport

import java.net.URLEncoder

actual fun encodeQueryParam(value: String): String = URLEncoder.encode(value, "UTF-8")
