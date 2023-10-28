package com.mahmoudhamdyae.camerax.ui

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mahmoudhamdyae.camerax.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "CameraXApp"
private var recording: Recording? = null

@Composable
fun CameraXApp() {
    val context = LocalContext.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
        }
    }

    var isShowingVisual by rememberSaveable { mutableStateOf(false) }
    var visualUri by rememberSaveable { mutableStateOf(Uri.EMPTY) }
    var flashOn by rememberSaveable { mutableStateOf(false) }

    BackHandler(
        enabled = isShowingVisual
    ) {
        if (isShowingVisual) isShowingVisual = false
    }

    if (!isShowingVisual) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            CameraPreview(
                controller = controller,
                modifier = Modifier
                    .fillMaxSize()
            )

            HeaderIcons(
                controller = controller,
                switchCameraAction = {
                    controller.cameraSelector =
                        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                },
                flashOn = flashOn,
                switchFlash = { flashOn = !flashOn }
            )

            BottomIcons(
                controller = controller,
                openImage = {
                    visualUri = it
                    isShowingVisual = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)

            )
        }
    } else {
        // Image or Video Screen
        if (isImage(context, visualUri)) {
            PhotoScreen(
                imageUri = visualUri,
                onBackButtonClicked = { isShowingVisual = !isShowingVisual },
            )
        } else {
            VideoScreen(
                videoUri = visualUri
            )
        }
    }
}

@Composable
fun HeaderIcons(
    controller: LifecycleCameraController,
    switchCameraAction: () -> Unit,
    flashOn: Boolean,
    switchFlash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = .3f))
            .padding(8.dp)
    ) {
        // Switch Camera Icon Button
        IconButton(
            onClick = switchCameraAction
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = stringResource(id = R.string.switch_camera_content_description),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Flash Icon Button
        IconButton(
            onClick = {
                switchFlash()
                controller.imageCaptureFlashMode = if (!flashOn) FLASH_MODE_ON else FLASH_MODE_OFF
            }
        ) {
            Icon(
                imageVector = if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = stringResource(R.string.flash_content_description),
                tint = Color.White
            )
        }
    }
}

@Composable
fun BottomIcons(
    controller: LifecycleCameraController,
    openImage: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val outputDirectory: File by remember {
        mutableStateOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
    }
    var isRecording by rememberSaveable { mutableStateOf(false) }

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                openImage(uri)
            }
        }

    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceTint.copy(alpha = .6f))
            .padding(8.dp)
    ) {
        // Open Gallery Icon Button
        IconButton(
            onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
                      },
            enabled = !isRecording
        ) {
            Icon(
                imageVector = Icons.Default.Photo,
                contentDescription = stringResource(R.string.open_gallery_content_description),
                tint = Color.White
            )
        }

        // Take Photo Icon Button
        IconButton(
            onClick = {
                takePhoto(
                    context = context,
                    controller = controller,
                    onPhotoTaken = openImage,
                    outputDirectory = outputDirectory
                )
            },
            enabled = !isRecording
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = stringResource(R.string.take_photo_content_description),
                tint = Color.White
            )
        }

        // Record Video Icon Button
        IconButton(
            onClick = {
                recordVideo(
                    context = context,
                    controller = controller,
                    outputDirectory = outputDirectory,
                    onVideoTaken = openImage
                )
                isRecording = !isRecording
            }
        ) {
            Icon(
                imageVector = if (!isRecording) Icons.Default.Videocam else Icons.Default.Stop,
                contentDescription = stringResource(R.string.record_video_content_description),
                tint = if (!isRecording) Color.White else Color.Red
            )
        }
    }
}

private fun takePhoto(
    context: Context,
    controller: LifecycleCameraController,
    onPhotoTaken: (Uri) -> Unit,
    outputDirectory: File,
) {
    val photoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".png"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    controller.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object: OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onPhotoTaken(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "onError: Couldn't take photo", exception)
                photoFile.delete()
            }

        }
    )
}

@SuppressLint("MissingPermission")
private fun recordVideo(
    context: Context,
    controller: LifecycleCameraController,
    outputDirectory: File,
    onVideoTaken: (Uri) -> Unit
) {
    if (recording != null) {
        recording?.stop()
        recording = null
        return
    }

    val videoFile = File(
        outputDirectory,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"
    )

    recording = controller.startRecording(
        FileOutputOptions.Builder(videoFile).build(),
        AudioConfig.create(true),
        ContextCompat.getMainExecutor(context),
    ) { event ->
        when (event) {
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    recording?.close()
                    recording = null
                    videoFile.delete()

                    Toast.makeText(
                        context,
                        context.getString(R.string.video_capture_failed),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    onVideoTaken(event.outputResults.outputUri)
                }
            }
        }
    }
}

private fun isImage(context: Context, uri: Uri): Boolean {
    val extension: String? = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        val mime = MimeTypeMap.getSingleton()
        mime.getExtensionFromMimeType(context.contentResolver.getType(uri))
    } else {
        // This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
        val uriString = Uri.fromFile(File(uri.path!!)).toString()
        MimeTypeMap.getFileExtensionFromUrl(uriString)
    }
    return (
            extension.equals("png") ||
                    extension.equals("jpg") ||
                    extension.equals("jpeg") ||
                    extension.equals("gif")
            )
}