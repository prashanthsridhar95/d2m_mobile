package com.d2m.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.QuickLook.QLPreviewController
import platform.QuickLook.QLPreviewControllerDataSourceProtocol
import platform.QuickLook.QLPreviewItemProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSInteger
import platform.darwin.NSObject

private class DocItem(private val url: NSURL, private val title: String) : NSObject(), QLPreviewItemProtocol {
    override fun previewItemURL(): NSURL = url
    override fun previewItemTitle(): String = title
}

private class DocSource(val item: DocItem) : NSObject(), QLPreviewControllerDataSourceProtocol {
    override fun numberOfPreviewItemsInPreviewController(controller: QLPreviewController): NSInteger = 1
    override fun previewController(controller: QLPreviewController, previewItemAtIndex: NSInteger): QLPreviewItemProtocol = item
}

// QLPreviewController holds its data source weakly, so keep the current one alive for as long as the preview can be on screen.
private var currentSource: DocSource? = null

@OptIn(ExperimentalForeignApi::class)
actual fun openDocument(fileName: String, mime: String, bytes: ByteArray): Boolean {
    val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "document" }
    val dir = NSTemporaryDirectory() + "opened_documents/"
    val fm = NSFileManager.defaultManager
    fm.removeItemAtPath(dir, error = null)
    fm.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    val url = NSURL.fileURLWithPath(dir + safe)
    val data = if (bytes.isEmpty()) NSData() else bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    if (!data.writeToURL(url, atomically = true)) return false

    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    val source = DocSource(DocItem(url, fileName)).also { currentSource = it }
    val preview = QLPreviewController().apply { dataSource = source }
    top.presentViewController(preview, animated = true, completion = null)
    return true
}
