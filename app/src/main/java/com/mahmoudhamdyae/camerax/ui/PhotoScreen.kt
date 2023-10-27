package com.mahmoudhamdyae.camerax.ui

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mahmoudhamdyae.camerx.R

@Composable
fun PhotoScreen(
    imageUri: Uri,
    onBackButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var offset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }

    var isBackButtonVisible by rememberSaveable { mutableStateOf(true) }

    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentScale = ContentScale.FillWidth,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            zoom = if (zoom > 1f) 1f else 2f
                            offset = calculateDoubleTapOffset(zoom, size, tapOffset)
                        },
                        onTap = { isBackButtonVisible = !isBackButtonVisible }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures(
                        onGesture = { centroid, pan, gestureZoom, _ ->
                            offset = offset.calculateNewOffset(
                                centroid, pan, zoom, gestureZoom, size
                            )
                            zoom = maxOf(1f, zoom * gestureZoom)
                        }
                    )
                }
                .graphicsLayer {
                    translationX = -offset.x * zoom
                    translationY = -offset.y * zoom
                    scaleX = zoom; scaleY = zoom
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        )
    }

    // Back Icon Button
    AnimatedVisibility(
        visible = isBackButtonVisible,
        enter = slideInVertically { -300 },
        exit = slideOutVertically { -300 }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = .6f))
                .padding(8.dp)
        ) {
            IconButton(
                onClick = onBackButtonClicked,
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.open_gallery_content_description),
                    tint = Color.White
                )
            }
        }
    }
}

private fun Offset.calculateNewOffset(
    centroid: Offset,
    pan: Offset,
    zoom: Float,
    gestureZoom: Float,
    size: IntSize
): Offset {
    val newScale = maxOf(1f, zoom * gestureZoom)
    val newOffset = (this + centroid / zoom) -
            (centroid / newScale + pan / zoom)
    return Offset(
        newOffset.x.coerceIn(0f, (size.width / zoom) * (zoom - 1f)),
        newOffset.y.coerceIn(0f, (size.height / zoom) * (zoom - 1f))
    )
}

private fun calculateDoubleTapOffset(
    zoom: Float,
    size: IntSize,
    tapOffset: Offset
): Offset {
    val newOffset = Offset(tapOffset.x, tapOffset.y)
    return Offset(
        newOffset.x.coerceIn(0f, (size.width / zoom) * (zoom - 1f)),
        newOffset.y.coerceIn(0f, (size.height / zoom) * (zoom - 1f))
    )
}