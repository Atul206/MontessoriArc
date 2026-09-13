package com.calmcoloring.app.ui.share

/**
 * Caption shown in the [ShareSheet] preview and sent as the share intent's
 * text. Blank/whitespace-only names fall back to a generic caption so an
 * empty "Artist's name" field never produces "Painted by " with nothing
 * after it.
 */
fun captionFor(name: String): String =
    name.trim().let { if (it.isEmpty()) "A little artist's painting" else "Painted by $it" }

/** Where a share can be routed. See `platform.shareArtwork` for the per-platform behavior. */
enum class ShareTarget { WhatsApp, Instagram, More }
