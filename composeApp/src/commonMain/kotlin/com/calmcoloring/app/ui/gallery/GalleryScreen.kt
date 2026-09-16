package com.calmcoloring.app.ui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.calmcoloring.app.generated.resources.Res
import com.calmcoloring.app.generated.resources.app_display_name
import com.calmcoloring.app.generated.resources.gallery_subtitle
import com.calmcoloring.app.generated.resources.legal_link_text
import com.calmcoloring.app.model.Template
import com.calmcoloring.app.platform.openUrl
import com.calmcoloring.app.ui.canvas.drawTemplate
import org.jetbrains.compose.resources.stringResource

// Hosted alongside this repo's other GitHub Pages content (see
// design/calm-coloring-ui-mockup.html) — no backend, just a static page.
private const val LEGAL_URL = "https://atul206.github.io/MontessoriArc/legal/"

/**
 * A lightweight, non-interactive render of [template]'s line art for use in
 * a gallery card preview. Deliberately a plain `Canvas` with no
 * `pointerInput` — unlike `RegionCanvas`, this must never intercept taps, so
 * the card's own `clickable` (navigation) is the only gesture handler in
 * play. Shares the fit/scale/draw logic with `RegionCanvas` via
 * `drawTemplate` so the two never drift apart.
 */
@Composable
private fun TemplatePreview(template: Template, modifier: Modifier = Modifier) {
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    Canvas(modifier = modifier) {
        drawTemplate(
            template = template,
            fills = emptyMap(),
            unfilledColor = Color.Transparent,
            outlineColor = outlineColor,
        )
    }
}

/**
 * The template gallery: a 2-column grid of cards, each tinted with its
 * template's accent color, tapping through to that template's coloring
 * screen via [onTemplateSelected].
 */
@Composable
fun GalleryScreen(
    templates: List<Template>,
    onTemplateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // safeDrawingPadding on the outer container keeps the title, the grid's
    // first/last row, and the footer all clear of the status/navigation bars
    // in edge-to-edge mode (android-skills:edge-to-edge) — matching
    // ColoringScreen.kt's established pattern in this codebase. Only this
    // single inset-consuming modifier is applied per hierarchy branch, so
    // the grid's own contentPadding stays a flat 16.dp with no separate
    // safeDrawing merge (avoids double-padding).
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Text(
            stringResource(Res.string.app_display_name),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        LazyVerticalGrid(
            // Adaptive rather than Fixed(2): a fixed 2-column grid left a
            // huge tablet width (e.g. a 2560px landscape tablet) rendering
            // just 2 giant cards — Adaptive fits as many 160dp-minimum
            // cards as the available width allows, so phones still get 2
            // columns while tablets naturally get 4-6.
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(templates, key = { it.id }) { template ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onTemplateSelected(template.id) }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(template.accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        TemplatePreview(
                            template = template,
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(template.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text(
            stringResource(Res.string.gallery_subtitle),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.legal_link_text),
            style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openUrl(LEGAL_URL) }
                .padding(16.dp),
            textAlign = TextAlign.Center,
        )
    }
}
