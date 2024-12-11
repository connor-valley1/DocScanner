# DocScanner

# CameraX Tutorial
Welcome to my CameraX tutorial! In this tutorial we will be implementing a basic demo application which utilizes the CameraX feature in android to view the camera preview, and capture and save an image to the phone's photos. This is a very basic implementation but goes over the basics of how to get started using the device camera from within your application.

## Getting Started
*You will need to create a new empty views activity in Android Studio*
The first step in using the CameraX is going to be to get the dependencies added into your empty views activity so that we can use all of the features in the demo app.

In your **App Level** build.gradle file you are going to want to include the following dependencies for CameraX:
```
val camerax_version = "1.3.0-alpha06"
implementation("androidx.camera:camera-camera2:$camerax_version")
implementation("androidx.camera:camera-lifecycle:$camerax_version")
implementation("androidx.camera:camera-view:$camerax_version")
```
*Optional Dependency for material icons:*
```
implementation (libs.androidx.material.icons.extended)
```
Used for addding a camera icon as the button to capture the image.

## Step 1:
Step 1 is to request the user's permission to access their camera. In order for us to use the camera, we first need to add the following code into our AndroidManifest.xml file located in the manifests folder of the app.

This code should go after the default ```xmlns:tools="http://schemas.android.com/tools``` line but before the application block:
```
<uses-feature
        android:name="android.hardware.camera"
        android:required="false" />
    <uses-permission android:name="android.permission.CAMERA"/>
```
This tells the device that our app is going to need to use the camera hardware of the device, it also defines the camera permission that we are going to need to get from the user so that we can access their camera.


Now on to the real stuff. In your MainActivity class, before the onCreate function, create a new private val for the requestPermissionsLauncher:
```
private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("DemoApp", "Camera Permission Granted")
        } else {
            Log.i("DemoApp", "Camera Permission Denied")
        }
    }
``` 
This code is our launcher for requesting permission, and it has a callback flag that we can use to determine whether the user granted us access to the camera or not, we Log this in the phone's logs so that we can view it in the Logcat within Android Studio.

Next we need a function to be able to use this launcher. We are going to create the following function inside the MainActivity class:
```
private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                Log.i("DemoApp", "Camera Permission Previously Granted")
            }
        
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.CAMERA
            ) -> { Log.i("DemoApp", "Show Camera Permission Rationale") }
        
            else -> {
                requestCameraPermissionLauncher.launch(
                    android.Manifest.permission.CAMERA
                )
            }
        }
}
```
This function checks if the user has previously granted us access to the camera, and also displays the Request Permission Rationale to request access to the camera if necessary. All we need to do from here is to call the function inside the onCreate block using ```requestCameraPermission()```.

After adding this code to your app, you should be able to run it and see the popup on the device asking if you want to allow access to the camera. Awesome!

## Step 2
Now that we have permission to access the camera, we want to be able to view the camera's preview! To do this we are going to create a new **@Composable** function inside our MainActivity:
```
@Composable
fun CameraPreview(
        modifier: Modifier = Modifier
) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val previewView = remember { PreviewView(context) }
}
```
I have started off with creating a few of the variables we are going to need for displaying the camera preview, as well as for binding the cameraProvider to the lifecycle.

We want to add a LaunchedEffect to his function, that operates on the creation of the PreviewView, this is where we are going to be getting the camera bound to the lifecycle, and get access to view the phone's camera feed:
```
LaunchedEffect(previewView) {
    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
    cameraProvider.unbindAll()

    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    try {
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview
        )
    } catch (exc: Exception) {
        Log.e("DemoApp", "Use case binding failed", exc)
    }
}
```
This code gets a new cameraProvider instance, and then unbinds all previously bound camera instances, as we are only allowed to have one instance of the camera bound. We are creating the preview here based on the previewView's surface provider, and then binding the cameraProvider to the lifecycle in a try block, as this could cause errors.

Now to get the actual content on the screen! We are going to create some composable elements to help with alignment and to display the screen and image capture button:
```
Box (contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
    AndroidView(
        factory = { ctx ->
            previewView
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    )
    IconButton(
        modifier = Modifier.padding(bottom = 12.dp),
        onClick = {
            Log.i("DemoApp", "Take Photo")
            //takePhoto(context, imageCapture)
        },
        content = {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = "Take Picture",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        }
    )
}
```
This code will create an AndroidView with context of previewView to display the camera's preview, and an IconButton at the bottom of the screen which is the button to capture the image. At this point, the takePhoto() function is commented out since we havent implemented that yet. 

However, at this point in the implemenation you can run the application and you should see the camera's preview displayed on the screen, along with the camera button at the bottom center.

With all of this working, lets move forward to preparing for image capture!

## Step 3
Now that we have the camera preview, we need to add a few more variables to get us ready to implement the takePhoto() function. First, we need to get a cameraExecutor variable that will be passed in to CameraX's takePicture() function. At the top of our file, we will declare our cameraExecutor, and then we will set it to a value in the onCreate() function:
```private lateinit var cameraExecutor: ExecutorService```
Setup in onCreate():
```cameraExecutor = Executors.newSingleThreadExecutor()```

Awesome! Now we have just one more variable that we will need for the takePhoto function. This comes inside our **@Composable** CameraPreview() function that we created in Step 2.

Near the top of the file where ```context```, ```lifecycleOwner```, and ```previewView``` are defined, add another variable definition:
```
val imageCapture = remember {
    ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()
}
```
This imageCapture variable creates a use-case of ImageCapture that we will use in the takePhoto() function. After adding this variable, we also need to bind it to the lifecycle with our other variables. Update the ```cameraProvider.bindtoLifecycle()``` block like this:
```
cameraProvider.bindToLifecycle(
    lifecycleOwner,
    CameraSelector.DEFAULT_BACK_CAMERA,
    preview,
    imageCapture // add imageCapture here
)
```

With this, we are now ready to implement the takePhoto() function!

## Step 4
Here we are going to be implementing the takePhoto() function, start off by creating the function with the following parameters:
```
private fun takePhoto(context: android.content.Context, imageCapture: ImageCapture) {

}
```
Now that we have the function created we can start by setting up the filename, and the file saving location. We do this using the following code:
```
val name = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.US)
    .format(System.currentTimeMillis())

val contentValues = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, name)
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Demo-Image")
    }
}
```
```name``` takes the current time and formats it into a readable date for our filename.
```contentValues``` specifies where we want to save the file, and what filetype we are going to use. It also checks for outdated versions of android that may have different file structure handling.

Next, we need to set up some output options to pass into the takePicture() function of cameraX, we do this using ```ImageCapture.OutputFileOptions```:
```
val outputOptions = ImageCapture.OutputFileOptions
    .Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )
    .build()
```
We are then going to pass these options, along with our ```cameraExecutor``` we made early into the takePicture() function:
*Call takePicture on imageCapture*
```
imageCapture.takePicture(
    outputOptions,
    cameraExecutor,
    object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
            Log.e("DemoApp", "Photo capture failed: ${exc.message}", exc)
        }
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            Log.i("DemoApp", "Photo capture succeeded: ${output.savedUri}")
        }
    }
)
```
This function takes in our options and handles the image capturing and file saving. We have two override callback functions, ```onError``` and ```onImageSaved```, if the image is saved successfully, we Log that it was successful, and output the URI for the file. If it fails, we will Log that it failed and output the exception that was thrown.

## Conclusion
After finishing step 4, you should have a working CameraX application which displays the camera preview, has a photo button to take the picture, and saves the image to the phone's photos library upon click. The app itself doesn't really provide feedback telling you that an image was taken, so make sure the check the Logs and look in the photos application on the device to find your pictures! This app was a very simple and basic outline of how to use the CameraX feature, but the feature itself can be used in many different was and for a lot of different things.

If you wanted to create a document scanner app such as the one featured in this repository, there are some third-party libraries that could make the process for implementing it a lot simpler and easier. One that I saw a lot while researching was using Google's ML Kit library. The ML Kit library has a built in document scanner function that works without you even needing to mess with CameraX at all. Open cv also showed up a lot when I was looking into automatic edge detection, and could likely be a good tool to use for making a similar but smarter app than my DocScanner implementation.

Thank you for reading my tutorial, I hope you learned something about android and about the CameraX feature that you can use in your next application!
