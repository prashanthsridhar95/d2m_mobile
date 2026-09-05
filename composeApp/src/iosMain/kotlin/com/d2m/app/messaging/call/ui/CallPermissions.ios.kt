package com.d2m.app.messaging.call.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.AVFAudio.AVAudioSession
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Requests microphone (and camera) access up front, mirroring the Android
 * actual's RequestMultiplePermissions and gating the call on the MIC result
 * specifically -- camera only matters once a call is upgraded to video, so
 * a denied camera must not block an audio call, on either platform.
 *
 * This used to be `{ onResult(true) }`, on the stated reasoning that iOS
 * prompts lazily the first time AVAudioSession is touched and that calling
 * was not wired up on iOS anyway. The second half is no longer true (WebRTC
 * and CallKit are both real here now), and the first half makes for a
 * genuinely worse experience than Android's: the prompt would land in the
 * middle of connecting a call, and a denial would surface as a call that
 * silently fails with no audio rather than as a clear "permission refused"
 * the caller can act on. Asking first makes the two platforms behave the
 * same way.
 *
 * Both callbacks are marshalled back to the main queue: AVFoundation
 * delivers them on an arbitrary internal queue, and `onResult` leads
 * directly into Compose state updates and call setup.
 */
@Composable
actual fun rememberCallPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val latestOnResult = rememberUpdatedState(onResult)
    return remember {
        {
            // Camera is requested first and its result deliberately ignored,
            // so that a video upgrade later in the call does not trigger a
            // second prompt mid-conversation -- same "request both upfront"
            // rationale as the Android actual.
            if (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) != AVAuthorizationStatusAuthorized) {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { }
            }
            AVAudioSession.sharedInstance().requestRecordPermission { micGranted ->
                dispatch_async(dispatch_get_main_queue()) {
                    latestOnResult.value(micGranted)
                }
            }
        }
    }
}
