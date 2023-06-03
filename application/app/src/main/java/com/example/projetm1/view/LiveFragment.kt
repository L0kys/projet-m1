package com.example.projetm1.view

import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.projetm1.R
import com.example.projetm1.databinding.LiveFragmentBinding
import com.example.projetm1.ml.Model
import com.example.projetm1.ml.ModelLstm
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class LiveFragment: Fragment(){

    private lateinit var binding: LiveFragmentBinding

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var movementTab = arrayOf(0,0,0)

    private lateinit var cameraExecutor: ExecutorService

    // Base pose detector with streaming frames, when depending on the pose-detection sdk
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val poseDetector = PoseDetection.getClient(options)

    // Select back camera as a default
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    var byteBufferLstm : ByteBuffer = ByteBuffer.allocateDirect(4*24*440)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        binding = LiveFragmentBinding.inflate(inflater)
        return binding.root
    }
    @ExperimentalGetImage
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val previewView = binding.viewFinder

        binding.resetButton.setOnClickListener {
            resetResult()
        }


        cameraExecutor = Executors.newSingleThreadExecutor()

        previewView.post {

            startCamera()
        }


    }


    private fun resetResult() {
        movementTab = arrayOf(0, 0, 0)
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

                                    // On récupère tous les points utiles pour notre modèle
                                    val leftShoulderX = it.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position3D?.x?.div(image.height)
                                    val leftShoulderY = it.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position3D?.y?.div(image.width)
                                    val rightShoulderX = it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.x?.div(image.height)
                                    val rightShoulderY = it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.y?.div(image.width)
                                    val leftElbowX = it.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.position3D?.x?.div(image.height)
                                    val leftElbowY = it.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.position3D?.y?.div(image.width)
                                    val rightElbowX = it.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.position3D?.x?.div(image.height)
                                    val rightElbowY = it.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.position3D?.y?.div(image.width)
                                    val leftWristX = it.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.position3D?.x?.div(image.height)
                                    val leftWristY = it.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.position3D?.y?.div(image.width)
                                    val rightWristX = it.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.position3D?.x?.div(image.height)
                                    val rightWristY = it.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.position3D?.y?.div(image.width)
                                    val leftHipX = it.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position3D?.x?.div(image.height)
                                    val leftHipY = it.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position3D?.y?.div(image.width)
                                    val rightHipX = it.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position3D?.x?.div(image.height)
                                    val rightHipY = it.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position3D?.y?.div(image.width)
                                    val leftKneeX = it.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.position3D?.x?.div(image.height)
                                    val leftKneeY = it.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.position3D?.y?.div(image.width)
                                    val rightKneeX = it.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.position3D?.x?.div(image.height)
                                    val rightKneeY = it.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.position3D?.y?.div(image.width)
                                    val leftAnkleX = it.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.position3D?.x?.div(image.height)
                                    val leftAnkleY = it.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.position3D?.y?.div(image.width)
                                    val rightAnkleX = it.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.position3D?.x?.div(image.height)
                                    val rightAnkleY = it.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.position3D?.y?.div(image.width)

                                    // On charge le modèle
                                    val model = Model.newInstance(requireContext())

                                    // On crée un buffer pour y metter les données utiles
                                    val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 24), DataType.FLOAT32)
                                    var byteBuffer : ByteBuffer = ByteBuffer.allocateDirect(4*24)
                                    byteBuffer.order(ByteOrder.nativeOrder())


                                    if (leftShoulderX != null) {
                                        byteBuffer.putFloat(leftShoulderX)
                                    }
                                    if (leftShoulderY != null) {
                                        byteBuffer.putFloat(leftShoulderY)
                                    }
                                    if (rightShoulderX != null) {
                                        byteBuffer.putFloat(rightShoulderX)
                                    }
                                    if (rightShoulderY != null) {
                                        byteBuffer.putFloat(rightShoulderY)
                                    }
                                    if (leftElbowX != null) {
                                        byteBuffer.putFloat(leftElbowX)
                                    }
                                    if (leftElbowY != null) {
                                        byteBuffer.putFloat(leftElbowY)
                                    }
                                    if (rightElbowX != null) {
                                        byteBuffer.putFloat(rightElbowX)
                                    }
                                    if (rightElbowY != null) {
                                        byteBuffer.putFloat(rightElbowY)
                                    }
                                    if (leftWristX != null) {
                                        byteBuffer.putFloat(leftWristX)
                                    }
                                    if (leftWristY != null) {
                                        byteBuffer.putFloat(leftWristY)
                                    }
                                    if (rightWristX != null) {
                                        byteBuffer.putFloat(rightWristX)
                                    }
                                    if (rightWristY != null) {
                                        byteBuffer.putFloat(rightWristY)
                                    }
                                    if (leftHipX != null) {
                                        byteBuffer.putFloat(leftHipX)
                                    }
                                    if (leftHipY != null) {
                                        byteBuffer.putFloat(leftHipY)
                                    }
                                    if (rightHipX != null) {
                                        byteBuffer.putFloat(rightHipX)
                                    }
                                    if (rightHipY != null) {
                                        byteBuffer.putFloat(rightHipY)
                                    }
                                    if (leftKneeX != null) {
                                        byteBuffer.putFloat(leftKneeX)
                                    }
                                    if (leftKneeY != null) {
                                        byteBuffer.putFloat(leftKneeY)
                                    }
                                    if (rightKneeX != null) {
                                        byteBuffer.putFloat(rightKneeX)
                                    }
                                    if (rightKneeY != null) {
                                        byteBuffer.putFloat(rightKneeY)
                                    }
                                    if (leftAnkleX != null) {
                                        byteBuffer.putFloat(leftAnkleX)
                                    }
                                    if (leftAnkleY != null) {
                                        byteBuffer.putFloat(leftAnkleY)
                                    }
                                    if (rightAnkleX != null) {
                                        byteBuffer.putFloat(rightAnkleX)
                                    }
                                    if (rightAnkleY != null) {
                                        byteBuffer.putFloat(rightAnkleY)
                                    }

                                    inputFeature0.loadBuffer(byteBuffer)

                                    // On lance le modèle avec les données récupérées par MLKit
                                    val outputs = model.process(inputFeature0)
                                    val outputFeature0 = outputs.outputFeature0AsTensorBuffer
                                    model.close()


                                    if (outputFeature0.floatArray[0] > 0.97f ){
                                        movementTab[0] += 1
                                    }
                                    else if (outputFeature0.floatArray[1] > 0.97f ){
                                        movementTab[1] += 1
                                    }
                                    else if (outputFeature0.floatArray[2] > 0.97f ){
                                        movementTab[2] += 1
                                    }

                                    if (movementTab[0] > movementTab[1] && movementTab[0] > movementTab[2]){
                                        binding.livePreviewExerciceNameTextView.text = getString(R.string.deadlift_name)
                                    } else if (movementTab[1] > movementTab[0] && movementTab[1] > movementTab[2]){
                                        binding.livePreviewExerciceNameTextView.text = getString(R.string.squat_name)
                                    }else{
                                        binding.livePreviewExerciceNameTextView.text = getString(R.string.bench_name)
                                    }
                                    // outputFeature0.floatArray[0]: Deadlift
                                    // outputFeature0.floatArray[1]: Squat
                                    // outputFeature0.floatArray[2]: Bench
                                    Log.d("Coucou", "leftShoulderX : "+ leftShoulderX + "  leftShoulderY : " + leftShoulderY)
                                    Log.d("quoicoubeh", "DeadliftCount : "+movementTab[0])
                                    Log.d("quoicoubeh", "SquatCount : "+movementTab[1])
                                    Log.d("quoicoubeh", "BenchCount : "+movementTab[2])
                                    Log.d("quoicoubeh", "Deadlift : "+outputFeature0.floatArray[0].toString())
                                    Log.d("quoicoubeh", "Squat : "+outputFeature0.floatArray[1].toString())
                                    Log.d("quoicoubeh", "Bench : "+outputFeature0.floatArray[2].toString())
                                    Log.d("quoicoubeh", "Undefined : "+outputFeature0.floatArray[3].toString())

/*
                                    // On charge le modèle
                                    val modelLstm = ModelLstm.newInstance(requireContext())
                                    val inputFeatureLstm = TensorBuffer.createFixedSize(intArrayOf(1, 440, 24), DataType.FLOAT32)
                                    var byteBufferTemp : ByteBuffer = ByteBuffer.allocateDirect(4*24*439)
                                    byteBufferTemp.order(ByteOrder.nativeOrder())


                                    byteBufferLstm.position(96)
                                    byteBufferTemp = byteBufferLstm.slice()

                                    byteBufferLstm.position(0)
                                    byteBufferLstm.put(byteBufferTemp)


                                    if (leftShoulderX != null) {
                                        byteBufferLstm.putFloat(leftShoulderX)
                                    }
                                    if (leftShoulderY != null) {
                                        byteBufferLstm.putFloat(leftShoulderY)
                                    }
                                    if (rightShoulderX != null) {
                                        byteBufferLstm.putFloat(rightShoulderX)
                                    }
                                    if (rightShoulderY != null) {
                                        byteBufferLstm.putFloat(rightShoulderY)
                                    }
                                    if (leftElbowX != null) {
                                        byteBufferLstm.putFloat(leftElbowX)
                                    }
                                    if (leftElbowY != null) {
                                        byteBufferLstm.putFloat(leftElbowY)
                                    }
                                    if (rightElbowX != null) {
                                        byteBufferLstm.putFloat(rightElbowX)
                                    }
                                    if (rightElbowY != null) {
                                        byteBufferLstm.putFloat(rightElbowY)
                                    }
                                    if (leftWristX != null) {
                                        byteBufferLstm.putFloat(leftWristX)
                                    }
                                    if (leftWristY != null) {
                                        byteBufferLstm.putFloat(leftWristY)
                                    }
                                    if (rightWristX != null) {
                                        byteBufferLstm.putFloat(rightWristX)
                                    }
                                    if (rightWristY != null) {
                                        byteBufferLstm.putFloat(rightWristY)
                                    }
                                    if (leftHipX != null) {
                                        byteBufferLstm.putFloat(leftHipX)
                                    }
                                    if (leftHipY != null) {
                                        byteBufferLstm.putFloat(leftHipY)
                                    }
                                    if (rightHipX != null) {
                                        byteBufferLstm.putFloat(rightHipX)
                                    }
                                    if (rightHipY != null) {
                                        byteBufferLstm.putFloat(rightHipY)
                                    }
                                    if (leftKneeX != null) {
                                        byteBufferLstm.putFloat(leftKneeX)
                                    }
                                    if (leftKneeY != null) {
                                        byteBufferLstm.putFloat(leftKneeY)
                                    }
                                    if (rightKneeX != null) {
                                        byteBufferLstm.putFloat(rightKneeX)
                                    }
                                    if (rightKneeY != null) {
                                        byteBufferLstm.putFloat(rightKneeY)
                                    }
                                    if (leftAnkleX != null) {
                                        byteBufferLstm.putFloat(leftAnkleX)
                                    }
                                    if (leftAnkleY != null) {
                                        byteBufferLstm.putFloat(leftAnkleY)
                                    }
                                    if (rightAnkleX != null) {
                                        byteBufferLstm.putFloat(rightAnkleX)
                                    }
                                    if (rightAnkleY != null) {
                                        byteBufferLstm.putFloat(rightAnkleY)
                                    }

                                    inputFeatureLstm.loadBuffer(byteBufferLstm)

                                    val outputs1 = modelLstm.process(inputFeatureLstm)
                                    val outputFeature1 = outputs1.outputFeature0AsTensorBuffer

                                    Log.d("quoicoubeh", "False Deadlift : "+outputFeature1.floatArray[0].toString())
                                    Log.d("quoicoubeh", "False Squat : "+outputFeature1.floatArray[1].toString())
                                    Log.d("quoicoubeh", "False Bench : "+outputFeature1.floatArray[2].toString())
                                    Log.d("quoicoubeh", "True Deadlift : "+outputFeature1.floatArray[0].toString())
                                    Log.d("quoicoubeh", "True Squat : "+outputFeature1.floatArray[1].toString())
                                    Log.d("quoicoubeh", "True Bench : "+outputFeature1.floatArray[2].toString())


                                    modelLstm.close()

 */
                                }


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

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}