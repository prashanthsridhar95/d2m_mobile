package com.d2m.app.messaging

import android.media.MediaPlayer
import com.d2m.app.R
import com.d2m.app.data.session.AndroidSettingsContextHolder

/**
 * MediaPlayer-backed implementation -- res/raw/{call_tone,msg_tone,
 * inchat_tune}.mp3 are the exact same files as d2m_web's public/sounds/
 * (copied verbatim, same names). MediaPlayer.create() plays them at normal
 * media/notification volume with default audio attributes, same as a plain
 * HTMLAudioElement -- deliberately NOT the ringtone-stream AudioAttributes
 * push/LocalNotificationBridge.android.kt uses for the background call
 * notification (that distinction matters for a literal-incoming-call
 * override of the ringer; an in-app tone playing while the person is
 * already looking at the app doesn't need it).
 */
actual object SoundEffects {
    private var callPlayer: MediaPlayer? = null

    private fun context() = AndroidSettingsContextHolder.appContext

    actual fun playCallTone() {
        stopCallTone()
        callPlayer = runCatching {
            MediaPlayer.create(context(), R.raw.call_tone)?.apply {
                isLooping = true
                start()
            }
        }.getOrNull()
    }

    actual fun stopCallTone() {
        runCatching {
            callPlayer?.stop()
            callPlayer?.release()
        }
        callPlayer = null
    }

    actual fun playMsgTone() = playOneShot(R.raw.msg_tone)

    actual fun playInChatTone() = playOneShot(R.raw.inchat_tune)

    // Fresh MediaPlayer per call, released on completion -- mirrors web's
    // "new Audio() per call" comment: messages can arrive in a quick burst
    // and overlapping one-shot plays are fine here, unlike the ringtone's
    // single reused/looping instance above.
    private fun playOneShot(resId: Int) {
        runCatching {
            val player = MediaPlayer.create(context(), resId) ?: return
            player.setOnCompletionListener { it.release() }
            player.start()
        }
    }
}
