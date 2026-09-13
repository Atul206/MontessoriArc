package com.calmcoloring.app.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.calmcoloring.app.platform.shareArtwork

/**
 * Bottom sheet shown after `ColoringScreen`'s share button captures the
 * current artwork. Every target tap (WhatsApp/Instagram/More) is gated
 * behind [ParentGateDialog] (Task 7) — [shareArtwork] only runs once the
 * gate's [onPassed][ParentGateDialog] fires, never directly from a button
 * click.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(artwork: ImageBitmap, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var pendingTarget by remember { mutableStateOf<ShareTarget?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Share this picture", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Card {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        bitmap = artwork,
                        contentDescription = null,
                        modifier = Modifier.size(128.dp),
                    )
                    Text(captionFor(name), style = MaterialTheme.typography.bodyMedium)
                    Text("Made with Calm Coloring", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Artist's name") },
                placeholder = { Text("e.g. Maya") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().imePadding(),
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareTarget.entries.forEach { target ->
                    OutlinedButton(
                        onClick = { pendingTarget = target },
                        modifier = Modifier.weight(1f),
                    ) { Text(target.name) }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Sharing sends only this picture — nothing else ever leaves the device.",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }

    pendingTarget?.let { target ->
        ParentGateDialog(
            onPassed = {
                shareArtwork(artwork, captionFor(name), target)
                pendingTarget = null
            },
            onDismiss = { pendingTarget = null },
        )
    }
}
