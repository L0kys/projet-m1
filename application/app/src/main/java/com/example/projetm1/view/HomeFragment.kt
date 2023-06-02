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
import com.example.projetm1.GraphicOverlay
import com.example.projetm1.R
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


@AndroidEntryPoint
class HomeFragment: Fragment(){

    private lateinit var binding: HomeFragmentBinding

    private val FILENAMEFORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    val modelName = "model.tflite"
    private var ratio: Float = 1.625f

    private lateinit var cameraExecutor: ExecutorService

    // Base pose detector with streaming frames, when depending on the pose-detection sdk
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val poseDetector = PoseDetection.getClient(options)

    // Select back camera as a default
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA



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
        val previewView = binding.viewFinder

        binding.videoCaptureButton.setOnClickListener {
            captureVideo()
        }




        cameraExecutor = Executors.newSingleThreadExecutor()

        previewView.post {


            Log.d("coucou", ratio.toString() + " 1")
            Log.d("coucou", binding.viewFinder.width.toString() + " width")
            Log.d("coucou", binding.viewFinder.height.toString() + " height")
            startCamera()
        }

    }

    // Implements VideoCapture use case, including start and stop capturing.
    @ExperimentalGetImage
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
            cameraProvider = cameraProviderFuture.get()

            try {
                cameraProvider!!.unbindAll()
                bindPreview()
                bindImageAnalysis()

            } catch(exc: Exception) {
                Log.e("Binding", "Use case binding failed", exc)
            }

        }, context?.let { ContextCompat.getMainExecutor(it) })
    }

    private fun bindPreview() {
        if (cameraProvider == null) {
            return
        }
        if (preview != null) {
            cameraProvider!!.unbind(preview)
        }

        val builder = Preview.Builder()

        preview = builder.build()
        preview!!.setSurfaceProvider(binding.viewFinder!!.surfaceProvider)
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, preview)
    }

    @ExperimentalGetImage
    private fun bindImageAnalysis() {
        if (cameraProvider == null) {
            return
        }
        if (imageAnalysis != null) {
            cameraProvider!!.unbind(imageAnalysis)
        }

        Log.d("coucou", ratio.toString() + " 2")
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(binding.viewFinder.width, binding.viewFinder.height))
            .build()


        context?.let { ContextCompat.getMainExecutor(it) }?.let {
            imageAnalysis.setAnalyzer(it, ImageAnalysis.Analyzer { imageProxy ->
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val image = imageProxy.image

                    if (image != null) {
                        val processImage = InputImage.fromMediaImage(image, rotationDegrees)
                        poseDetector.process(processImage)
                            .addOnSuccessListener { it ->
                                if(binding.parentLayout.childCount>3){
                                    binding.parentLayout.removeViewAt(3)
                                }
                                if(it.allPoseLandmarks.isNotEmpty()){

                                    if(binding.parentLayout.childCount>3){
                                        binding.parentLayout.removeViewAt(3)
                                    }

                                    val element = Draw(context,it,image.height,image.width)
                                    binding.parentLayout.addView(element)
                                }

                                // Creates inputs for reference.
                                val inputFeature = it.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position3D?.x.toString() +" "+ it.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position3D?.y.toString() +" "+ it.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position3D?.z.toString() +" "+ it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.toString()

                                val test = it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.x?.div(
                                    image.height
                                ).toString() + " " + it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.y?.div(
                                    image.width
                                ).toString() + "         " + image.height + " " + image.width + "         " + it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.x.toString() + " " + it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.y.toString() + " " + it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.z.toString()
                                Log.d("coucou", test)
                                //Log.d("coucou", inputFeature)
                                // Runs model inference and gets result.
                                // val outputs = model?.process(inputFeature0)
                                // val outputFeature0 = outputs?.outputFeature0AsTensorBuffer

                                // Releases model resources if no longer used.
                                // model?.close()
                                imageProxy.close()
                            }
                            .addOnFailureListener {
                                imageProxy.close()
                            }
                    }

            })
        }

        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, imageAnalysis)
    }
    @ExperimentalGetImage
    private fun bindCamera() {
        if (cameraProvider == null) {
            return
        }
        if (videoCapture != null) {
            cameraProvider!!.unbind(videoCapture)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, videoCapture)
    }

    private fun unBindPreview(){
        cameraProvider!!.unbind(preview)
    }

    private fun unBindCamera(){
        cameraProvider!!.unbind(videoCapture)
    }

    private fun unBindAll(){
        cameraProvider!!.unbindAll()
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}