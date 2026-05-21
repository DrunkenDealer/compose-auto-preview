package io.github.drunkendealer.composeautopreview.sample

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import io.github.drunkendealer.composeautopreview.annotations.AutoPreview
import io.github.drunkendealer.composeautopreview.annotations.Device
import io.github.drunkendealer.composeautopreview.annotations.Theme

data class ConfirmDialogState(
    val title: String,
    val message: String,
    val confirmLabel: String = "OK",
    val dismissLabel: String? = "Cancel",
)

object ConfirmDialogSampleData {
    val Default = ConfirmDialogState(
        title = "Delete item?",
        message = "This action cannot be undone.",
    )
    val DestructiveLong = ConfirmDialogState(
        title = "Delete account",
        message = "Your account and all associated data will be permanently removed. " +
            "This cannot be undone and we will not be able to recover it for you.",
        confirmLabel = "Delete forever",
        dismissLabel = "Keep account",
    )
    val NoDismiss = ConfirmDialogState(
        title = "Update required",
        message = "A new version is available. Please update to continue.",
        confirmLabel = "Update",
        dismissLabel = null,
    )
}

@Composable
fun ConfirmDialog(state: ConfirmDialogState, onConfirm: () -> Unit = {}, onDismiss: () -> Unit = {}) {
    MaterialTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(state.title) },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(state.confirmLabel) } },
            dismissButton = state.dismissLabel?.let { label ->
                { TextButton(onClick = onDismiss) { Text(label) } }
            },
        )
    }
}

@AutoPreview(
    samplesFrom = ConfirmDialogSampleData::class,
    devices = [Device.Phone, Device.Tablet],
    themes = [Theme.Light, Theme.Dark],
)
@ConfirmDialogPreviewAutoPreviews
@Composable
internal fun ConfirmDialogPreview(
    @PreviewParameter(ConfirmDialogPreviewSamplesProvider::class) state: ConfirmDialogState,
) = ConfirmDialog(state)
