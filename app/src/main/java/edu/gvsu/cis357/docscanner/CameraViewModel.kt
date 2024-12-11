package edu.gvsu.cis357.docscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.File

class CameraViewModel : ViewModel() {
    // ImageCapture instance for capturing images
    private var imageCapture: ImageCapture? = null

    // set up the camera, bind preview and image capture
    fun setupCamera(previewView: PreviewView, context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // create a camera preview instance
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            // create an image capture instance
            imageCapture = ImageCapture.Builder().build()

            // use back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // unbind previous instances before binding
                cameraProvider.unbindAll()

                // bind the preview and image capture to lifecycle
                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                // handle errors with binding
                Log.e("CameraViewModel", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // capture image and save it to a file
    fun captureImage(context: Context, onImageSaved: (Uri?) -> Unit) {
        val imageCapture = imageCapture ?: return


        // create a file for the captured image to be stored
        val photoFile = File(
            context.getExternalFilesDir(null),
            "document_${System.currentTimeMillis()}.jpg"
        )
        Log.d("CameraViewModel", "Attempting to save file at: ${photoFile.absolutePath}")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // take image and save it
        imageCapture.takePicture(
            outputOptions,
            Dispatchers.IO.asExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraViewModel", "File successfully saved at: $savedUri")
                    // callback with saved uri
                    onImageSaved(Uri.fromFile(photoFile))
                }
                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    // callback with null due to error
                    onImageSaved(null)
                }
            }
        )
    }

    // crop the image based on the placement of corner points
    fun cropImage(
        context: Context,
        imageUri: Uri,
        points: List<Offset>,
        onCropped: (Uri?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalBitmap = getBitmapFromUri(context, imageUri) ?: return@launch

                // get the dimensions of original bitmap
                val originalWidth = originalBitmap.width.toFloat()
                val originalHeight = originalBitmap.height.toFloat()

                // get screen dimensions
                val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
                val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()

                // determine how the bitmap fits within screen dimensions
                val bitmapAspectRatio = originalWidth / originalHeight
                val screenAspectRatio = screenWidth / screenHeight

                val displayedWidth: Float
                val displayedHeight: Float
                val horizontalPadding: Float
                val verticalPadding: Float

                if (bitmapAspectRatio > screenAspectRatio) {
                    // if bitmap is wider than the screen, apply padding to height
                    displayedWidth = screenWidth
                    displayedHeight = screenWidth / bitmapAspectRatio
                    horizontalPadding = 0f
                    verticalPadding = (screenHeight - displayedHeight) / 2f
                } else {
                    // if bitmap is taller than the screen, apply padding to width
                    displayedHeight = screenHeight
                    displayedWidth = screenHeight * bitmapAspectRatio
                    verticalPadding = 0f
                    horizontalPadding = (screenWidth - displayedWidth) / 2f
                }

                // offset adjustment values to align cropped image with corner points
                val fineTuneOffsetX = 35f
                val fineTuneOffsetY = 30f

                // scale the points from screen coordinates to bitmap coordinates
                val scaledPoints = points.map { point ->
                    Offset(
                        x = (((point.x - horizontalPadding) + fineTuneOffsetX) / displayedWidth * originalWidth)
                            .coerceIn(0f, originalWidth),
                        y = (((point.y - verticalPadding) + fineTuneOffsetY) / displayedHeight * originalHeight)
                            .coerceIn(0f, originalHeight)
                    )
                }

                // calculate the cropping boundaries
                val left = scaledPoints.minOf { it.x }.toInt().coerceAtLeast(0)
                val top = scaledPoints.minOf { it.y }.toInt().coerceAtLeast(0)
                val right = scaledPoints.maxOf { it.x }.toInt().coerceAtMost(originalBitmap.width)
                val bottom = scaledPoints.maxOf { it.y }.toInt().coerceAtMost(originalBitmap.height)

                val cropWidth = (right - left).coerceAtLeast(1)
                val cropHeight = (bottom - top).coerceAtLeast(1)

                if (cropWidth > 0 && cropHeight > 0) {
                    val croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, cropWidth, cropHeight)
                    val croppedUri = saveBitmapToUri(context, croppedBitmap)
                    // callback with cropped image uri
                    onCropped(croppedUri)
                } else {
                    Log.e("CropDebug", "Invalid cropping dimensions")
                    // callback with null due to error
                    onCropped(null)
                }
            } catch (e: Exception) {
                Log.e("CropDebug", "Failed to crop image", e)
                // callback with null due to error
                onCropped(null)
            }
        }
    }

    // function to save a bitmap as a JPEG file and return the URI, used to save the cropped image
    fun saveBitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        return try {
            // create the file for the cropped image to be stored
            val file = File(
                context.getExternalFilesDir(null),
                "cropped_image_${System.currentTimeMillis()}.jpg"
            )

            // set up and save cropped image to file
            val outputStream = file.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // return the URI of the cropped image file
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // function to save the final cropped bitmap as a PDF file and return the URI
    fun saveImageAsPdf(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return try {
            // convert to a software-based bitmap for saving
            val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            // set the directory for saving PDF scans, create one if it doesn't exist
            val scansFolder = File(context.getExternalFilesDir(null), "scans")
            if (!scansFolder.exists()) {
                scansFolder.mkdirs()
            }
            // create the file for the PDF to be saved, taking in filename from user
            val pdfFile = File(scansFolder, "$fileName.pdf")

            // create the PDF document
            val pdfDocument = PdfDocument()

            // add page to PDF document
            val pageInfo = PdfDocument.PageInfo.Builder(softwareBitmap.width, softwareBitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // draw the software-based bitmap on the page
            canvas.drawBitmap(softwareBitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            // write the PDF document to the file
            pdfFile.outputStream().use {
                pdfDocument.writeTo(it)
            }
            pdfDocument.close()

            // log and return the URI of the saved PDF file
            Log.d("PDFSave", "PDF successfully saved at: ${pdfFile.absolutePath}")
            Uri.fromFile(pdfFile)
        } catch (e: Exception) {
            // log error with saving PDF and return null
            Log.e("PDFSave", "Error saving PDF", e)
            null
        }
    }
}