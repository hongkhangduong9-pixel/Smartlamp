package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.example.model.ThemeSettings
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SkyAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private data class LoadedBackgroundAssets(
    val sharp: ImageBitmap,
    val topBlurred: ImageBitmap?,
    val bottomBlurred: ImageBitmap?,
)

@Composable
fun ThemeBackground(
    theme: ThemeSettings,
    modifier: Modifier = Modifier,
) {
    val topRadius = theme.topBlurDp.roundToInt().coerceIn(0, 40)
    val bottomRadius = theme.bottomBlurDp.roundToInt().coerceIn(0, 40)

    val loadedAssets by produceState<LoadedBackgroundAssets?>(
        initialValue = null,
        key1 = theme.backgroundImagePath,
        key2 = topRadius,
        key3 = bottomRadius,
    ) {
        val path = theme.backgroundImagePath
        if (path.isNullOrBlank() || !File(path).exists()) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            decodeAndPrepareBlurAssets(path, topRadius, bottomRadius)
        }
    }

    val blurColor = Color(theme.blurColorArgb)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val assets = loadedAssets

        if (assets != null) {
            val imageModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = theme.scale
                    scaleY = theme.scale
                    translationX = theme.offsetXFraction * containerWidthPx
                    translationY = theme.offsetYFraction * containerHeightPx
                }

            // Base crisp photo
            Image(
                bitmap = assets.sharp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )

            // Upper progressive blur + color tint
            if (assets.topBlurred != null && theme.topBlurDp > 0.5f) {
                val topAlpha = (theme.topBlurDp / 40f).coerceIn(0.15f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = topAlpha),
                                    0.22f to Color.Black.copy(alpha = topAlpha * 0.75f),
                                    0.48f to Color.Transparent,
                                    1.0f to Color.Transparent,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        },
                ) {
                    Image(
                        bitmap = assets.topBlurred,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to blurColor.copy(alpha = 0.45f * topAlpha),
                                    0.45f to Color.Transparent,
                                )
                            ),
                    )
                }
            }

            // Lower progressive blur + color tint
            if (assets.bottomBlurred != null && theme.bottomBlurDp > 0.5f) {
                val bottomAlpha = (theme.bottomBlurDp / 40f).coerceIn(0.15f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.52f to Color.Transparent,
                                    0.78f to Color.Black.copy(alpha = bottomAlpha * 0.75f),
                                    1.0f to Color.Black.copy(alpha = bottomAlpha),
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        },
                ) {
                    Image(
                        bitmap = assets.bottomBlurred,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.55f to Color.Transparent,
                                    1.0f to blurColor.copy(alpha = 0.5f * bottomAlpha),
                                )
                            ),
                    )
                }
            }

            // Readability scrim overlay preserving card and text contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(blurColor.copy(alpha = theme.overlayOpacity.coerceIn(0f, 0.85f))),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.30f),
                            0.25f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.25f),
                        )
                    ),
            )
        } else {
            // Default atmospheric smart-home backdrop when no custom photo is set
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0F1A),
                                Color(0xFF101828),
                                Color(0xFF090D16),
                            )
                        )
                    )
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AmberGlow.copy(alpha = 0.11f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.18f, size.height * 0.14f),
                                radius = size.maxDimension * 0.52f,
                            ),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SkyAccent.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.85f, size.height * 0.82f),
                                radius = size.maxDimension * 0.58f,
                            ),
                        )
                    },
            )
        }
    }
}

private fun decodeAndPrepareBlurAssets(
    path: String,
    topRadius: Int,
    bottomRadius: Int,
): LoadedBackgroundAssets? {
    val opts = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = BitmapFactory.decodeFile(path, opts) ?: return null
    val sharpImg = decoded.asImageBitmap()

    val topBlurred = if (topRadius > 0) {
        createFastBlurredBitmap(decoded, (topRadius / 2).coerceIn(2, 24)).asImageBitmap()
    } else null

    val bottomBlurred = if (bottomRadius > 0) {
        if (bottomRadius == topRadius && topBlurred != null) {
            topBlurred
        } else {
            createFastBlurredBitmap(decoded, (bottomRadius / 2).coerceIn(2, 24)).asImageBitmap()
        }
    } else null

    return LoadedBackgroundAssets(
        sharp = sharpImg,
        topBlurred = topBlurred,
        bottomBlurred = bottomBlurred,
    )
}

/**
 * Downscales source bitmap and runs a fast 2-pass separable box blur,
 * delivering smooth progressive blur across all Android API levels (24-36).
 */
private fun createFastBlurredBitmap(source: Bitmap, radius: Int): Bitmap {
    val targetWidth = 220
    val aspect = source.height.toFloat() / source.width.coerceAtLeast(1).toFloat()
    val targetHeight = (targetWidth * aspect).roundToInt().coerceIn(64, 420)
    val small = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    return stackBlur(small, radius)
}

private fun stackBlur(bitmap: Bitmap, radius: Int): Bitmap {
    if (radius < 1) return bitmap
    val w = bitmap.width
    val h = bitmap.height
    val pix = IntArray(w * h)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)

    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1

    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    val vmin = IntArray(maxOf(w, h))

    var divsum = (div + 1) shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    for (i in dv.indices) {
        dv[i] = i / divsum
    }

    var yw = 0
    var yi = 0
    val stack = Array(div) { IntArray(3) }
    val r1 = radius + 1

    for (y in 0 until h) {
        var rsum = 0
        var gsum = 0
        var bsum = 0
        var routsum = 0
        var goutsum = 0
        var boutsum = 0
        var rinsum = 0
        var ginsum = 0
        var binsum = 0

        for (i in -radius..radius) {
            val p = pix[yi + minOf(wm, maxOf(i, 0))]
            val sir = stack[i + radius]
            sir[0] = (p and 0xff0000) shr 16
            sir[1] = (p and 0x00ff00) shr 8
            sir[2] = p and 0x0000ff
            val rbs = r1 - kotlin.math.abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
        }
        var stackpointer = radius
        for (x in 0 until w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            val stackstart = stackpointer - radius + div
            val sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (y == 0) {
                vmin[x] = minOf(x + radius + 1, wm)
            }
            val p = pix[yw + vmin[x]]
            sir[0] = (p and 0xff0000) shr 16
            sir[1] = (p and 0x00ff00) shr 8
            sir[2] = p and 0x0000ff
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            val sirNext = stack[stackpointer % div]
            routsum += sirNext[0]
            goutsum += sirNext[1]
            boutsum += sirNext[2]
            rinsum -= sirNext[0]
            ginsum -= sirNext[1]
            binsum -= sirNext[2]
            yi++
        }
        yw += w
    }

    for (x in 0 until w) {
        var rsum = 0
        var gsum = 0
        var bsum = 0
        var routsum = 0
        var goutsum = 0
        var boutsum = 0
        var rinsum = 0
        var ginsum = 0
        var binsum = 0
        var yp = -radius * w
        for (i in -radius..radius) {
            yi = maxOf(0, yp) + x
            val sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            val rbs = r1 - kotlin.math.abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) {
                yp += w
            }
        }
        yi = x
        var stackpointer = radius
        for (y in 0 until h) {
            pix[yi] = (0xff shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            val stackstart = stackpointer - radius + div
            val sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (x == 0) {
                vmin[y] = minOf(y + r1, hm) * w
            }
            val p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            val sirNext = stack[stackpointer]
            routsum += sirNext[0]
            goutsum += sirNext[1]
            boutsum += sirNext[2]
            rinsum -= sirNext[0]
            ginsum -= sirNext[1]
            binsum -= sirNext[2]
            yi += w
        }
    }

    bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    return bitmap
}
