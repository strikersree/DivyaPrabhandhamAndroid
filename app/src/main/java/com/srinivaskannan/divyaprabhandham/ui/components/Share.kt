package com.srinivaskannan.divyaprabhandham.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Shares a pasuram as plain text.
 *
 * SwiftUI's `ShareLink` has no Compose equivalent; the platform way is a chooser
 * over ACTION_SEND, which is what every other Android app does and what people
 * expect from the share affordance.
 */
fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

/**
 * Shares a rendered pasuram card as a PNG.
 *
 * The bitmap is written into the cache under share_cards/ and handed over as
 * a content:// URI through the app's FileProvider: ACTION_SEND cannot carry a
 * file:// path on any supported Android version, and a provider is the only
 * way to give one other app read access to one file without giving every app
 * access to the directory.
 *
 * The caption rides along as EXTRA_TEXT. Apps that take both (WhatsApp,
 * Telegram) prefill it beside the image; apps that take only one take the
 * image, which is what was asked for.
 *
 * The file keeps the pasuram's own name and is overwritten next time rather
 * than accumulating: the cache is not a gallery, and the person already has
 * the image in whatever they shared it to.
 */
fun sharePasuramImage(context: Context, bitmap: Bitmap, fileName: String, caption: String) {
    val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
    val file = File(dir, fileName)
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
