package com.d2m.app.messaging

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

/**
 * AVAudioPlayer-backed implementation, mirroring `SoundEffects.android.kt`
 * one-for-one over the SAME three audio files (call_tone/msg_tone/
 * inchat_tune, copied verbatim from d2m_web's public/sounds/ into
 * `iosApp/iosApp/Sounds/` so they land in the app bundle -- see
 * iosApp/project.yml).
 *
 * Was a no-op stub, which meant the exact complaint that motivated
 * SoundEffects.kt in the first place ("Why notification sounds are not
 * working like how it's working on web?") was still fully true on iOS after
 * being fixed on Android: CallLayer's incoming-call UI looked like a
 * ringing phone with no audio behind it, and messages arrived silently
 * while the app was open.
 *
 * The category is deliberately Ambient rather than Playback: these are
 * in-app UI tones played while the user is already looking at the app, so
 * they should mix with (not interrupt) whatever else is playing and should
 * respect the hardware silent switch -- matching the Android actual's
 * choice of plain media attributes over the ringtone stream, and for the
 * same reason. The genuinely-interrupting incoming-call ring is CallKit's
 * job (IosCallKitBridge.kt), not this file's.
 */
@OptIn(ExperimentalForeignApi::class)
actual object SoundEffects {
    private var callPlayer: AVAudioPlayer? = null

    /** Kept alive for the duration of playback -- an AVAudioPlayer that goes out of scope stops immediately. */
    private val oneShotPlayers = mutableListOf<AVAudioPlayer>()

    private fun player(resource: String): AVAudioPlayer? {
        val path = NSBundle.mainBundle.pathForResource(resource, "mp3") ?: return null
        return AVAudioPlayer(contentsOfURL = NSURL.fileURLWithPath(path), error = null)
    }

    private fun activateSession() {
        runCatching {
            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, null)
            AVAudioSession.sharedInstance().setActive(true, null)
        }
    }

    actual fun playCallTone() {
        stopCallTone()
        runCatching {
            activateSession()
            callPlayer = player("call_tone")?.apply {
                numberOfLoops = -1 // loop for the whole "incoming" phase, same as the Android actual's isLooping
                play()
            }
        }
    }

    actual fun stopCallTone() {
        runCatching { callPlayer?.stop() }
        callPlayer = null
    }

    actual fun playMsgTone() = playOneShot("msg_tone")

    actual fun playInChatTone() = playOneShot("inchat_tune")

    /**
     * Fresh player per call, matching the Android actual and web's "new
     * Audio() per call": messages can arrive in a burst and overlapping
     * one-shot plays are correct here. Finished players are swept on the
     * next play rather than via a delegate -- simpler, and the list can only
     * ever hold the handful still actually sounding.
     */
    private fun playOneShot(resource: String) {
        runCatching {
            activateSession()
            oneShotPlayers.removeAll { !it.playing }
            player(resource)?.let {
                oneShotPlayers.add(it)
                it.play()
            }
        }
        return
    }
}
