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











