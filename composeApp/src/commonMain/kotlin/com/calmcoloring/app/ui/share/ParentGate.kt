package com.calmcoloring.app.ui.share

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ParentGateState(private val a: Int, private val b: Int) {
    val challenge: Pair<Int, Int> get() = a to b
    fun check(answer: Int): Boolean = answer == a + b

    companion object {
        fun random(): ParentGateState {
            val a = (3..9).random()
            val b = (2..8).random()
            return ParentGateState(a, b)
        }
    }
}

@Composable
fun ParentGateDialog(onPassed: () -> Unit, onDismiss: () -> Unit) {
    val gate = remember { ParentGateState.random() }
    var answer by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick check for grown-ups") },
        text = {
            Column {
                Text("What's ${gate.challenge.first} + ${gate.challenge.second}?")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; showError = false },
                    isError = showError,
                    supportingText = if (showError) { { Text("Not quite — try again.") } } else null,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = answer.toIntOrNull()
                if (parsed != null && gate.check(parsed)) onPassed() else showError = true
            }) { Text("Continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
