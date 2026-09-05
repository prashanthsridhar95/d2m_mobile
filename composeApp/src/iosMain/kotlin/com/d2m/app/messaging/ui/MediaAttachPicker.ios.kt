package com.d2m.app.messaging.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * Real attach picker, replacing a no-op launcher that made the composer's
 * paperclip button silently do nothing on iOS -- so sending any photo,
 * video or file was simply impossible there while Android had full support.
 *
 * Android's actual uses GetContent with an any-type filter, whose system picker surfaces
 * BOTH the photo library and general documents behind one entry point. iOS
 * has no single equivalent, so this presents the same choice explicitly as
 * an action sheet -- which is also exactly what mobile Safari does for the
 * unrestricted `<input type="file">` this feature is ported from
 * (MessageComposer.jsx), so it matches the web app's behaviour on iOS too:
 *  - "Photo Library" -> PHPickerViewController. Needs no permission prompt
 *    at all (it runs out of process and hands back only what the user
 *    actually picked), which is why it is preferred over the older
 *    UIImagePickerController.
 *  - "Browse Files" -> UIDocumentPickerViewController over UTTypeItem (any
 *    type), covering documents, iCloud Drive and anything non-media.
 *
 * The delegate is retained in a module-level reference for the lifetime of
 * the presentation: UIKit holds its `delegate` weakly, so a delegate kept
 * only in a Compose `remember` can be collected the moment the composable
 * recomposes, and the callback would then never fire.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberMediaAttachLauncher(onPicked: (PickedMedia) -> Unit): () -> Unit {
    val latestOnPicked = rememberUpdatedState(onPicked)
    return remember {
        { presentAttachSheet { picked -> latestOnPicked.value(picked) } }
    }
}

/** Strong reference to whichever delegate is currently presented -- see the class doc above for why this cannot live in `remember`. */
private var activeDelegate: NSObject? = null

private fun rootViewController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.let { generateSequence(it) { vc -> vc.presentedViewController }.last() }

@OptIn(ExperimentalForeignApi::class)
private fun presentAttachSheet(onPicked: (PickedMedia) -> Unit) {
    val root = rootViewController() ?: return
    // An action sheet on iPad is a popover and UIKit REQUIRES an anchor
    // (sourceView/barButtonItem) or it raises at presentation time. The
    // anchoring API is not exposed to Kotlin/Native here, so iPad uses the
    // alert style instead, which is self-positioning and needs no anchor.
    // TARGETED_DEVICE_FAMILY is "1,2" (see iosApp/project.yml), so iPad is a
    // real target and this is not a hypothetical.
    val isPad = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
    val sheet = UIAlertController.alertControllerWithTitle(
        title = null,
        message = null,
        preferredStyle = if (isPad) UIAlertControllerStyleAlert else UIAlertControllerStyleActionSheet,
    )
    sheet.addAction(
        UIAlertAction.actionWithTitle("Photo Library", UIAlertActionStyleDefault) {
            presentPhotoPicker(onPicked)
        },
    )
    sheet.addAction(
        UIAlertAction.actionWithTitle("Browse Files", UIAlertActionStyleDefault) {
            presentDocumentPicker(onPicked)
        },
    )
    sheet.addAction(UIAlertAction.actionWithTitle("Cancel", UIAlertActionStyleCancel, null))

    root.presentViewController(sheet, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun presentPhotoPicker(onPicked: (PickedMedia) -> Unit) {
    val root = rootViewController() ?: return
    val config = PHPickerConfiguration().apply { selectionLimit = 1 }
    val picker = PHPickerViewController(configuration = config)
    val delegate = PhotoPickerDelegate(onPicked)
    activeDelegate = delegate
    picker.delegate = delegate
    root.presentViewController(picker, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private class PhotoPickerDelegate(private val onPicked: (PickedMedia) -> Unit) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            activeDelegate = null
            return
        }
        val provider: NSItemProvider = result.itemProvider
        // The first registered identifier is the provider's preferred
        // representation (e.g. public.jpeg, public.movie) -- asking for that
        // specifically returns the ORIGINAL file bytes, rather than
        // re-encoding through UIImage the way UIImagePickerController would.
        val typeId = provider.registeredTypeIdentifiers.firstOrNull() as? String
        if (typeId == null) {
            activeDelegate = null
            return
        }
        val suggestedName = provider.suggestedName ?: "attachment"

        provider.loadDataRepresentationForTypeIdentifier(typeId) { data: NSData?, _: NSError? ->
            val bytes = data?.toByteArray()
            // Callbacks arrive on a private queue; every consumer of
            // onPicked ends up touching Compose state, so hop to main.
            dispatch_async(dispatch_get_main_queue()) {
                activeDelegate = null
                if (bytes != null) {
                    val mime = mimeForUti(typeId)
                    onPicked(
                        PickedMedia(
                            bytes = bytes,
                            fileName = fileNameFor(suggestedName, mime),
                            mime = mime,
                            kind = kindForMime(mime),
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun presentDocumentPicker(onPicked: (PickedMedia) -> Unit) {
    val root = rootViewController() ?: return
    // asCopy = true hands back a URL inside this app's own temp container,
    // so it can be read immediately without security-scoped-resource
    // bookkeeping.
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem), asCopy = true)
    val delegate = DocumentPickerDelegate(onPicked)
    activeDelegate = delegate
    picker.delegate = delegate
    root.presentViewController(picker, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private class DocumentPickerDelegate(private val onPicked: (PickedMedia) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        controller.dismissViewControllerAnimated(true, null)
        activeDelegate = null
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val bytes = NSData.dataWithContentsOfURL(url)?.toByteArray() ?: return
        val name = url.lastPathComponent ?: "attachment"
        val mime = mimeForExtension(url.pathExtension)
        onPicked(PickedMedia(bytes = bytes, fileName = name, mime = mime, kind = kindForMime(mime)))
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, null)
        activeDelegate = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return out
}

/** Same four-way bucketing the Android actual applies, and the vocabulary MessagingRepository.sendMedia expects. */
private fun kindForMime(mime: String): String = when {
    mime.startsWith("image/") -> "image"
    mime.startsWith("video/") -> "video"
    mime.startsWith("audio/") -> "audio"
    else -> "file"
}

/**
 * Minimal UTI -> MIME mapping for the identifiers PHPicker actually hands
 * back. Deliberately small rather than a full UTType lookup: the picker only
 * ever vends photo/video library items, and anything unrecognised falls
 * through to a generic type that MessagingRepository.sendMedia accepts
 * anyway.
 */
private fun mimeForUti(uti: String): String = when (uti) {
    "public.jpeg" -> "image/jpeg"
    "public.png" -> "image/png"
    "public.heic", "public.heif" -> "image/heic"
    "com.compuserve.gif" -> "image/gif"
    "public.webp" -> "image/webp"
    "com.apple.quicktime-movie" -> "video/quicktime"
    "public.mpeg-4" -> "video/mp4"
    "public.movie" -> "video/mp4"
    else -> "application/octet-stream"
}

private fun mimeForExtension(ext: String?): String = when (ext?.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "heic" -> "image/heic"
    "webp" -> "image/webp"
    "mp4" -> "video/mp4"
    "mov" -> "video/quicktime"
    "m4a" -> "audio/mp4"
    "mp3" -> "audio/mpeg"
    "wav" -> "audio/wav"
    "pdf" -> "application/pdf"
    "txt" -> "text/plain"
    else -> "application/octet-stream"
}

/** PHPicker's suggestedName has no extension; chat bubbles and downloads both read better with one. */
private fun fileNameFor(suggested: String, mime: String): String {
    if (suggested.contains('.')) return suggested
    val ext = when (mime) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/heic" -> "heic"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "video/quicktime" -> "mov"
        "video/mp4" -> "mp4"
        else -> return suggested
    }
    return "$suggested.$ext"
}
