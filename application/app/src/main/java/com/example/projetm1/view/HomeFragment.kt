package com.example.projetm1.view

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.example.projetm1.databinding.HomeFragmentBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.projetm1.R

@AndroidEntryPoint
class HomeFragment: Fragment(){

    private lateinit var binding: HomeFragmentBinding

    private val FILENAMEFORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    // Base pose detector with streaming frames, when depending on the pose-detection sdk
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    val poseDetector = PoseDetection.getClient(options)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        binding = HomeFragmentBinding.inflate(inflater)
        return binding.root
    }
    @ExperimentalGetImage
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.videoCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()
    }

    // Implements VideoCapture use case, including start and stop capturing.
    private fun captureVideo() {

        val videoCapture = this.videoCapture ?: return

        binding.videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAMEFORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val resolver = requireActivity().contentResolver

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = context?.let {
            videoCapture.output
                .prepareRecording(it, mediaStoreOutputOptions)
                .apply {
                    if (PermissionChecker.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.RECORD_AUDIO) ==
                        PermissionChecker.PERMISSION_GRANTED) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    when(recordEvent) {
                        is VideoRecordEvent.Start -> {
                            binding.videoCaptureButton.apply {
                                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_catching_pokemon_24))
                                isEnabled = true
                            }
                        }

                        is VideoRecordEvent.Finalize -> {
                            if (!recordEvent.hasError()) {
                                val msg = "Video capture succeeded: " +
                                        "${recordEvent.outputResults.outputUri}"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                recording?.close()
                                recording = null
                                Log.e("capture video", "Video capture ends with error: " +
                                        "${recordEvent.error}")
                            }
                            binding.videoCaptureButton.apply {
                                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_my_icon))
                                isEnabled = true
                            }
                        }
                    }
                }
        }
    }

    @ExperimentalGetImage
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this.requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(
                            Quality.SD)))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(480,640))
                .build()

            context?.let { ContextCompat.getMainExecutor(it) }?.let {
                imageAnalysis.setAnalyzer(it, ImageAnalysis.Analyzer { imageProxy ->
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val image = imageProxy.image

                    if(image!=null){
                        val processImage = InputImage.fromMediaImage(image,rotationDegrees)

                        poseDetector.process(processImage)
                            .addOnSuccessListener { it ->
                                Log.d("RIGHT_WRIST", it.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.position3D.toString())
                                imageProxy.close()
                            }
                            .addOnFailureListener{


                                imageProxy.close()
                            }
                    }


                })
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)

            } catch(exc: Exception) {
                Log.e("Binding", "Use case binding failed", exc)
            }

        }, context?.let { ContextCompat.getMainExecutor(it) })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}