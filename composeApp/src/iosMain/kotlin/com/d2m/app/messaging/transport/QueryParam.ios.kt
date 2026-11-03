package com.d2m.app.messaging.transport

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
// Confirmed directly against the real SDK header (NSURL.h): this is a class
// PROPERTY named `URLQueryAllowedCharacterSet` (capital URL, matching Apple's
// pre-Swift-naming-modernization ObjC declaration) -- not the lowercase-url
// Swift-style name this used to import.
import platform.Foundation.URLQueryAllowedCharacterSet

actual fun encodeQueryParam(value: String): String =
    (value as NSString).stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet) ?: value
