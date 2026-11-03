package com.d2m.app

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.fflush
import platform.posix.stdout

/**
 * Diagnostic-only, temporary -- installed from AppDelegate.didFinishLaunchingWithOptions
 * (iOSApp.swift) as the very first line, before KoinBootstrap/CallKit/anything
 * else. Kotlin/Native's crash reporter (the "Translated Report" xcrun simctl
 * shows) only ever prints NATIVE stack frames for an uncaught Kotlin
 * exception's abort -- it never includes the actual exception's message or
 * toString(), which is exactly the gap that made the SurfaceMetalRedrawer.draw
 * crash unactionable from the crash log alone.
 *
 * FIRST ATTEMPT used NSLog("...%@", kotlinString) -- confirmed directly this
 * SEGFAULTS (EXC_BAD_ACCESS inside CFStringCreateWithFormat's
 * objc_opt_respondsToSelector, crashing thread's own stack showed
 * CrashLogger$install$1.invoke -> NSLog -> __CFStringAppendFormatCore ->
 * objc_opt_respondsToSelector on a garbage pointer). Root cause: Kotlin/
 * Native's interop for calling an ObjC C-variadic function (NSLog's
 * `(NSString*, ...)`) with a plain Kotlin String vararg doesn't reliably
 * bridge the way `NSString`-literal-argument NSLog calls from real
 * Objective-C code do -- a known sharp edge, not something to fight further
 * here. Using plain `println` instead: Kotlin/Native's stdlib println
 * writes straight to stdout via the runtime's own C stdio path, with no
 * Objective-C message-send/varargs bridging at all, so it can't hit this
 * failure mode. iOS Simulator processes have their stdout captured by
 * `xcrun simctl launch --stdout=<file>` (also generally visible in the
 * unified system log / Xcode console).
 */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
object CrashLogger {
    fun install() {
        setUnhandledExceptionHook { throwable ->
            val sb = StringBuilder()
            sb.append("D2M_CRASH ==== UNCAUGHT KOTLIN EXCEPTION ====\n")
            sb.append("D2M_CRASH type=")
            sb.append(throwable::class.qualifiedName ?: throwable::class.simpleName ?: "unknown")
            sb.append('\n')
            sb.append("D2M_CRASH message=")
            sb.append(throwable.message ?: "<no message>")
            sb.append('\n')
            sb.append("D2M_CRASH toString=")
            sb.append(throwable.toString())
            sb.append('\n')
            var cause: Throwable? = throwable.cause
            var depth = 0
            while (cause != null && depth < 5) {
                sb.append("D2M_CRASH cause[")
                sb.append(depth)
                sb.append("]=")
                sb.append(cause.toString())
                sb.append('\n')
                cause = cause.cause
                depth++
            }
            println(sb.toString())
            // Kotlin/Native's own stack trace -- also plain stdio under the
            // hood, gives us the Kotlin-level (kfun:...) call chain.
            throwable.printStackTrace()
            // CRITICAL: stdout redirected to a file (xcrun simctl launch
            // --stdout=) is fully-buffered, not line-buffered -- and the
            // runtime calls abort() immediately after this hook returns,
            // which does NOT flush open stdio buffers on the way out.
            // Without this explicit flush, everything printed above is lost
            // the moment the process aborts (confirmed directly: previous
            // attempt's hook ran with no crash of its own, yet the redirected
            // stdout file was still empty afterward).
            fflush(stdout)
        }
    }
}
