package com.calmcoloring.app.ui.share

/**
 * Caption shown in the [ShareSheet] preview and sent as the share intent's
 * text. Blank/whitespace-only names fall back to [anonymousCaption] so an
 * empty "Artist's name" field never produces a "Painted by" caption with
 * nothing after it. [paintedByTemplate] is the raw (unformatted)
 * `caption_painted_by` resource string — its `%1$s` placeholder is
 * substituted here rather than via `stringResource`'s format-args overload,
 * since this is a plain (non-`@Composable`) function so it can stay unit
 * tested without a composition.
 */
fun captionFor(name: String, anonymousCaption: String, paintedByTemplate: String): String =
    name.trim().let { if (it.isEmpty()) anonymousCaption else paintedByTemplate.replace("%1\$s", it) }

/** Where a share can be routed. See `platform.shareArtwork` for the per-platform behavior. */
enum class ShareTarget { WhatsApp, Instagram, More }
