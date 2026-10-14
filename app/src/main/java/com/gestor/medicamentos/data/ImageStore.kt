package com.gestor.medicamentos.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** Guarda las fotos (perfil / caja del medicamento) en el almacenamiento interno de la app. */
object ImageStore {
    fun saveUri(ctx: Context, uri: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 1200) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        bmp?.let { saveBitmap(ctx, it) }
    }.getOrNull()

    fun saveBitmap(ctx: Context, bmp: Bitmap): String {
        val f = File(ctx.filesDir, "img_${System.nanoTime()}.jpg")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return f.absolutePath
    }
}
