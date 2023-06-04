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

/**
 * Ce fragment est l'un des deux fragments principaux où les modèles de décision sont importés.
 * Il permet d'avoir une vue en direct de la caméra avec un affichage instantané de la position qui est détectée
 * il fait aussi appel à la classe Draw pour pouvoir afficher la position du corps détectée en
 * surimpression de la preview.
 *
 * Dans ce fragment on a aussi importé le/les modèles TfLite qui permettent de détecter quel exercice est
 * pratiqué pour le NN ou si le mouvement est valide pour le LSTM
 * **/
@AndroidEntryPoint
class LiveFragment: Fragment(){

    private lateinit var binding: LiveFragmentBinding

    //création des variables qui contiendront les UseCases pour bind la caméra
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null

    //Tableau permettant le comptage de la détection du mouvement grâce au NN
    private var movementTab = arrayOf(0,0,0)


    private lateinit var cameraExecutor: ExecutorService

    // Création du PoseDetector de MLKIT qui permet l'inférence du squelette
    private val options = PoseDetectorOptions.Builder()
            // On utilise STREAM_MODE, adapté à la détection en temps réel, car on souhaite un maximum de vitesse d'inférence
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector = PoseDetection.getClient(options)

    // On sélectionne la caméra arrière du téléphone par défaut
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Création du buffer pour contenir l'entrée du LSTM
    //var byteBufferLstm : ByteBuffer = ByteBuffer.allocateDirect(4*24*440)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        binding = LiveFragmentBinding.inflate(inflater)
        return binding.root
    }

    // Après création du Fragment on créé le caméra exécutor et on assigne sa tâche au boutton de reset
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



    //Fonction qui va permettre de démarrer la preview de la caméra ainsi que l'analyse d'image en direct
    @ExperimentalGetImage
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this.requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            try {
                // On s'assure qu'il n'y a plus d'UseCases sur caméraProvider et on bind les deux dont on a besoin (preview + imageAnalysis)
                cameraProvider!!.unbindAll()
                bindPreview()
                bindImageAnalysis()

            } catch(exc: Exception) {
                Log.e("Binding", "Use case binding failed", exc)
            }

        }, context?.let { ContextCompat.getMainExecutor(it) })
    }

    // Fonction permettant un simple affichage de ce que filme la caméra du téléphone
    private fun bindPreview() {
        if (cameraProvider == null) {
            return
        }
        if (preview != null) {
            cameraProvider!!.unbind(preview)
        }

        val builder = Preview.Builder()

        preview = builder.build()
        // On affecte viewFinder comme surface d'affichage
        preview!!.setSurfaceProvider(binding.viewFinder!!.surfaceProvider)
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, preview)
    }

    // Cette fonction va simultanément créer le UseCase imageAnalysis et tout le traitement que nous effectuons sur chaque frame captée
    @ExperimentalGetImage
    private fun bindImageAnalysis() {
        if (cameraProvider == null) {
            return
        }
        if (imageAnalysis != null) {
            cameraProvider!!.unbind(imageAnalysis)
        }

        val imageAnalysis = ImageAnalysis.Builder()
                // Ici on choissit une stratégie de contre pression qui prend des frames dès que possible et abandonne celles qui ne peuvent pas être traitées
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                // On s'assure que la résolution corresponde à la taille allouée sur l'écran du téléphone
            .setTargetResolution(Size(binding.viewFinder.width, binding.viewFinder.height))
            .build()


        // Ici on a créer tout le processus qui va être appliqué à chaque frame traitée
        context?.let { ContextCompat.getMainExecutor(it) }?.let {
            imageAnalysis.setAnalyzer(it, ImageAnalysis.Analyzer { imageProxy ->
                //création de l'image dans le bon format grâce à caméraX
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val image = imageProxy.image

                    if (image != null) {
                        val processImage = InputImage.fromMediaImage(image, rotationDegrees)
                        // On donne l'image créée au pose detector de MLKIT qui sous réserve de succès nous renvoit un objet Pose contenant toutes les coordonnées que nous cherchons
                        poseDetector.process(processImage)
                            .addOnSuccessListener { it ->

                                //On affiche la vue supplémentaire crée à travers la classe Draw
                                if(binding.parentLayout.childCount>3){
                                    binding.parentLayout.removeViewAt(3)
                                }
                                if(it.allPoseLandmarks.isNotEmpty()){

                                    if(binding.parentLayout.childCount>3){
                                        binding.parentLayout.removeViewAt(3)
                                    }

                                    val element = Draw(context,it,image.height,image.width)
                                    binding.parentLayout.addView(element)


                                    // On récupère toutes les coordonnées des points utiles pour notre modèle
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

                                    // On crée un buffer pour y metter les données utiles dans le bon ordre
                                    val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 24), DataType.FLOAT32)
                                    var byteBuffer : ByteBuffer = ByteBuffer.allocateDirect(4*24)
                                    byteBuffer.order(ByteOrder.nativeOrder())

                                    // On ajoute les-dit points au Buffer
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

                                    // On incrémente la table des exercices
                                    if (outputFeature0.floatArray[0] > 0.97f ){
                                        movementTab[0] += 1
                                    }
                                    else if (outputFeature0.floatArray[1] > 0.97f ){
                                        movementTab[1] += 1
                                    }
                                    else if (outputFeature0.floatArray[2] > 0.97f ){
                                        movementTab[2] += 1
                                    }

                                    // On change le texte à l'écran en fonction de la classe dominante dans la session actuelle
                                    if (movementTab[0] > movementTab[1] && movementTab[0] > movementTab[2]){
                                        binding.livePreviewExerciceNameTextView.text = getString(R.string.deadlift_name)
                                    } else if (movementTab[1] > movementTab[0] && movementTab[1] > movementTab[2]){
                                        binding.livePreviewExerciceNameTextView.text = getString(R.string.squat_name)
                                    }else{
                                        binding.livePreviewExerciceNameTextView.text = getString(R.string.bench_name)
                                    }



                                    // On charge le modèleLSTM et on l'exploite
 /*                                   val modelLstm = ModelLstm.newInstance(requireContext())
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

                                    Log.d("test", "False Deadlift : "+outputFeature1.floatArray[0].toString())
                                    Log.d("test", "False Squat : "+outputFeature1.floatArray[1].toString())
                                    Log.d("test", "False Bench : "+outputFeature1.floatArray[2].toString())
                                    Log.d("test", "True Deadlift : "+outputFeature1.floatArray[0].toString())
                                    Log.d("test", "True Squat : "+outputFeature1.floatArray[1].toString())
                                    Log.d("test", "True Bench : "+outputFeature1.floatArray[2].toString())

                                    modelLstm.close()
*/
                                }

                                // Fermeture de l'image proxy importante pour que l'analyseur puisse avoir l'information qu'il peut chercher une nouvelle frame
                                imageProxy.close()
                            }
                            .addOnFailureListener {
                                imageProxy.close()
                            }
                    }

            })
        }

        // On bind le UseCase imageAnalysis au cycle de vie du caméraProvider
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, imageAnalysis)
    }

    // On ferme cameraExecutor quand la page est détruite
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    //Fonction qui remet le tableau à zéro si on souhaite voir une nouvelle détection de quel exercice est pratiqué
    private fun resetResult() {
        movementTab = arrayOf(0, 0, 0)
    }
}