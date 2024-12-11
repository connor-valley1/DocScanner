package edu.gvsu.cis357.docscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch


// composable function to display the camera preview, and handle image capture, cropping, and saving
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel(),
    navController: NavController
) {
    // local context and coroutine scope, for handling asynchronous actions
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // local variables used for image data and UI state
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var points by remember {
        mutableStateOf(
            calculateInitialPoints(screenWidth = 800f, screenHeight = 800f, boxSize = 300f)
        )
    }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fileName by remember { mutableStateOf("scan_${System.currentTimeMillis()}") }
    var isCropConfirmed by remember { mutableStateOf(false) }
    var isFinalActionConfirmed by remember { mutableStateOf(false) }

    // main container for the camera preview and UI elements
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (capturedImageUri == null) {
            // show camera preview, and a capture image button
            Text(
                text = "Align document in frame",
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f) // Fill the width of the screen, assuming a 3:4 aspect ratio
                    .align(Alignment.Center)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = androidx.camera.view.PreviewView(ctx).apply {
                            scaleType =
                                PreviewView.ScaleType.FIT_CENTER // Ensures the preview fills the width
                        }
                        coroutineScope.launch {
                            viewModel.setupCamera(previewView, context)
                        }
                        previewView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(3f / 4f)
                )
            }
        }

        // Display captured image with adjustable corner points
        capturedImageUri?.let { uri ->
            val bitmap = getBitmapFromUri(context, uri)
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    contentScale = ContentScale.FillWidth
                )
                if (!isCropConfirmed) {
                    // place adjustable corners for cropping
                    AdjustableCorners(
                        points = points,
                        onPointsChanged = { updatedPoints -> points = updatedPoints },
                        modifier = Modifier.fillMaxSize()
                    )

                    Text(
                        text = "Drag corners to adjust scan",
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                    )

                    // confirm button for cropping
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    ) {
                        IconButton(
                            onClick = {
                                // on button click, call cropImage function passing in local context, uri, and location of points
                                viewModel.cropImage(context, uri, points) { croppedUri ->
                                    if (croppedUri != null) {
                                        // if successfully cropped image, update capturedImageUri, croppedBitmap, and isCropConfirmed flag
                                        capturedImageUri = croppedUri
                                        isCropConfirmed = true

                                        croppedBitmap = getBitmapFromUri(context, croppedUri)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_document_scanner_24),
                                contentDescription = "Confirm Crop",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                } else if (!isFinalActionConfirmed) {
                    // display cropped image, rename file, and save file
                    Text(
                        text = "Scanned Document:",
                        fontSize = 36.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp)
                    )
                    // text field for filename entry
                    TextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("File Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    ) {
                        Button(
                            onClick = {
                                // on button click, save cropped image as a pdf file
                                isFinalActionConfirmed = true
                                croppedBitmap?.let {
                                    val pdfUri = viewModel.saveImageAsPdf(context, it, fileName)
                                    if (pdfUri != null) {
                                        Log.d("PDFSave", "PDF successfully saved at: $pdfUri")
                                    } else {
                                        Log.e("PDFSave", "Failed to save PDF")
                                    }
                                }
                                // navigate back to the main screen after pdf save
                                navController.navigate("main")
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .border(2.dp, Color.Black),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                        ) {
                            Text(
                                text = "Save File",
                                color = Color.Black,
                                fontSize = 24.sp
                            )
                        }
                    }

                }
            }
        }

        if (capturedImageUri == null) {
            // capture button for camera preview
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                IconButton(
                    onClick = {
                        // on button click, call captureImage function passing in local context and callback
                        viewModel.captureImage(
                            context = context,
                            onImageSaved = { uri ->
                                // save the captured image URI
                                capturedImageUri = uri
                            }
                        )
                    },
                    modifier = Modifier
                        .size(80.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.round_camera_24),
                        contentDescription = "Capture Image",
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

// helper function to convert a URI to a Bitmap for processing
fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// helper function to calculate the initial location for points
fun calculateInitialPoints(screenWidth: Float, screenHeight: Float, boxSize: Float): List<Offset> {
    val left = (screenWidth - boxSize) / 2
    val top = (screenHeight - boxSize) / 2
    return listOf(
        Offset(left, top),
        Offset(left + boxSize, top),
        Offset(left + boxSize, top + boxSize),
        Offset(left, top + boxSize)
    )
}

// composable for drawing the adjustable corners
@Composable
fun AdjustableCorners(
    points: List<Offset>,
    onPointsChanged: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier
) {
    // local state variables for points
    val updatedPoints = remember { mutableStateListOf(*points.toTypedArray()) }
    var selectedPointIndex by remember { mutableStateOf(-1) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // handle drag gestures for points
                detectDragGestures(
                    // detect if drag starts at a point
                    onDragStart = { dragStartOffset ->
                        // get the distance of drag, if less than 50f, select point
                        selectedPointIndex = updatedPoints.indexOfFirst { point ->
                            (point - dragStartOffset).getDistance() < 50f
                        }
                    },
                    // update the position of selected point
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (selectedPointIndex != -1) {
                            // adjust selected point based on drag
                            updatedPoints[selectedPointIndex] =
                                updatedPoints[selectedPointIndex] + dragAmount
                        }
                    },
                    // reset selected point, pass updated points list to parent
                    onDragEnd = {
                        selectedPointIndex = -1
                        onPointsChanged(updatedPoints.toList())
                    }
                )
            }) {
        // draw the points
        updatedPoints.forEach { point ->
            drawCircle(
                color = Color.Red,
                center = point,
                radius = 20f
            )
        }
        // draw the lines connecting points
        for (i in updatedPoints.indices) {
            val nextIndex = (i + 1) % updatedPoints.size
            drawLine(
                color = Color.Red,
                start = updatedPoints[i],
                end = updatedPoints[nextIndex],
                strokeWidth = 4f
            )
        }
    }
}