package com.srinivaskannan.divyaprabhandham.ui.components

import android.content.Context
import android.content.Intent

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
