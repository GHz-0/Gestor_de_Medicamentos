package com.gestor.medicamentos.ui.components

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.gestor.medicamentos.data.ImageStore

/** Muestra una foto recortada a [shape] o un texto/emoji de relleno. El tamaño lo da [modifier]. */
@Composable
fun ImageBox(path: String?, modifier: Modifier, shape: Shape = CircleShape, placeholder: String = "📷") {
    val bmp = remember(path) { path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
    Box(modifier.clip(shape).background(MaterialTheme.colorScheme.secondaryContainer), Alignment.Center) {
        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(placeholder, style = MaterialTheme.typography.headlineMedium)
    }
}

/** Foto opcional: cámara o galería (la cámara no requiere permiso porque usa la app de cámara del sistema). */
@Composable
fun PhotoPicker(
    path: String?, onChange: (String?) -> Unit, modifier: Modifier,
    shape: Shape = CircleShape, placeholder: String = "📷"
) {
    val ctx = LocalContext.current
    var dialog by remember { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { ImageStore.saveUri(ctx, it) }?.let { onChange(it) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let { ImageStore.saveBitmap(ctx, it) }?.let { onChange(it) }
    }
    ImageBox(path, modifier.clickable { dialog = true }, shape, placeholder)

    if (dialog) AlertDialog(
        onDismissRequest = { dialog = false },
        title = { Text("Fotografía") },
        text = {
            Column {
                TextButton({ dialog = false; camera.launch(null) }) { Text("📷  Tomar foto") }
                TextButton({ dialog = false; gallery.launch("image/*") }) { Text("🖼  Elegir de la galería") }
                if (path != null) TextButton({ dialog = false; onChange(null) }) { Text("🗑  Quitar foto") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton({ dialog = false }) { Text("Cancelar") } }
    )
}
