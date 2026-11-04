package com.choreme.feature.rooms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomPhoto
import java.io.ByteArrayOutputStream

/** The longest side of a stored picture, in pixels: plenty for a card, small enough to keep in the room. */
private const val MAX_SIDE = 960

/**
 * Reads the picture at [uri] and shrinks it to a small JPEG that fits in a room's document
 * ([Room.MAX_PHOTO_BYTES]). Null when it can't be read.
 */
internal fun readRoomPhoto(context: Context, uri: Uri): RoomPhoto? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val scale = MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
    val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true) else decoded
    var quality = 80
    var bytes: ByteArray
    do {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bytes = out.toByteArray()
        quality -= 10
    } while (bytes.size > Room.MAX_PHOTO_BYTES && quality >= 20)
    if (bytes.size > Room.MAX_PHOTO_BYTES) null else RoomPhoto(bytes)
}.getOrNull()

internal fun RoomPhoto.toBitmap(): Bitmap? = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
