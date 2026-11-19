package com.daftar.app.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

@Composable
fun OfflineQrCodeImage(
    payload: String,
    size: Dp = 140.dp,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(payload) {
        runCatching {
            val matrix = MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 320, 320)
            val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    bmp.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bmp
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(size),
            )
        } else {
            Text(payload)
        }
    }
}

@Composable
fun OfflineCode128BarcodeImage(
    code: String,
    width: Dp = 220.dp,
    height: Dp = 64.dp,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(code) {
        runCatching {
            val safe = code.ifBlank { "DFTR-0001" }
            val matrix = MultiFormatWriter().encode(safe, BarcodeFormat.CODE_128, 440, 110)
            val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    bmp.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bmp
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Barcode $code",
                modifier = Modifier.size(width, height),
            )
        } else {
            Text(code)
        }
    }
}
