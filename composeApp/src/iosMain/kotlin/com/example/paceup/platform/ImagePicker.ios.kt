package com.example.paceup.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * Launches UIImagePickerController to pick a photo from the gallery.
 * Result delivered via [ImagePickerResult].
 * iOS: verify on Mac before PR.
 * TODO(paceup): upgrade to PHPickerViewController (iOS 14+) in Task 11.x.
 */
actual class ImagePicker {
    actual fun launch() {
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.delegate = PickerDelegate
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
    }
}

private object PickerDelegate : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val uiImage = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val nsData: NSData? = uiImage?.let { UIImageJPEGRepresentation(it, 0.8) }
        if (nsData != null && nsData.length > 0u) {
            val bytes = ByteArray(nsData.length.toInt())
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
            }
            ImagePickerResult.submit(bytes)
        }
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}
