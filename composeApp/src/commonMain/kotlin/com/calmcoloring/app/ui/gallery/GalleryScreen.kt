package com.calmcoloring.app.ui.gallery

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmcoloring.app.model.Template

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
            "Calm Coloring",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
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
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(template.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text(
            "No sound · no ads · no accounts — just tap and color.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center,
        )
    }
}
