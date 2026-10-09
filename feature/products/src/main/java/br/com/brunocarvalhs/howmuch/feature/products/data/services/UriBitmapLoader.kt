package br.com.brunocarvalhs.howmuch.feature.products.data.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Decodes a content/file URI into a Bitmap off the main thread; null when it can't be read. */
class UriBitmapLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun load(imageUri: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(imageUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }.getOrNull()
    }
}
