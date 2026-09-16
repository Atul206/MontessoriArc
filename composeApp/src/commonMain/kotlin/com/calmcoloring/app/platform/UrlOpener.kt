package com.calmcoloring.app.platform

/**
 * Opens [url] in the system browser. Used for the gallery's "Privacy Policy
 * & Terms" link — the only outbound navigation in the app that isn't a
 * share/print action, so it gets its own small `expect`/`actual` rather than
 * reusing [shareArtwork]'s Intent machinery.
 */
expect fun openUrl(url: String)
