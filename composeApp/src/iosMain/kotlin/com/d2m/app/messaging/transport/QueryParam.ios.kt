package com.d2m.app.messaging.transport

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.Foundation.urlQueryAllowedCharacterSet

actual fun encodeQueryParam(value: String): String =
    (value as NSString).stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.urlQueryAllowedCharacterSet) ?: value
