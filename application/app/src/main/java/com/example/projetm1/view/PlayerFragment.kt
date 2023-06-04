package com.example.projetm1.view

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.projetm1.R
import com.example.projetm1.Utils
import com.example.projetm1.databinding.PlayerFragmentBinding
import com.example.projetm1.decoder.Frame
import com.example.projetm1.decoder.FrameExtractor
import com.example.projetm1.decoder.IVideoFrameExtractor
import com.example.projetm1.ml.Model
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * PlayerFragment nous permet d'afficher la vidéo selectionnée depuis le StorageFragment et également de réaliser notre post-process dessus.
 * Pour l'affichage de la vidéo nous utilisons ExoPlayer et pour le post-process nous utilisons un code trouvé sur github qui à été réalisé par "Duc Ky Ngo".
 *
 */


@AndroidEntryPoint
class PlayerFragment: Fragment(), IVideoFrameExtractor {

    //Initialise le poseDetector de MLKit
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val poseDetector = PoseDetection.getClient(options)
    private var movementTab = arrayOf(0,0,0)

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var imagePaths = ArrayList<Uri>()
    private var titles: ArrayList<String> = ArrayList()
    var totalSavingTimeMS: Long = 0

    companion object{
        private lateinit var player: ExoPlayer
        var position: Int = -1
    }

    private lateinit var binding: PlayerFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PlayerFragmentBinding.inflate(inflater)
        return binding.root
    }

    // Lorsque le fragment est chargé on affiche la vidéo et on cache les éléments non utiles pour l'instant
    override fun onViewCreated (view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayVideo()
        binding.resultLayout.visibility = INVISIBLE

        // Lorsque l'on click sur le boutton on lance le post-process
        binding.buttonTest.setOnClickListener{
            binding.buttonTest.isClickable = false
            binding.infoTextView.visibility = VISIBLE
            binding.resultLayout.visibility = INVISIBLE
            binding.exoName.text = getString(R.string.live_preview_text)
            postProcess()

        }
    }

    // Cette fonction permet de faire appel au FrameExtractor pour récupérer chaque frame de la vidéo
    private fun postProcess() {
        imagePaths.clear()
        titles.clear()
        totalSavingTimeMS = 0

        val frameExtractor = FrameExtractor(this)
        executorService.execute {
            try {
                frameExtractor.extractFrames(StorageFragment.videoList[position].artUri.toString().substring(5))
            } catch (exception: Exception) {
                exception.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to extract frames", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Cette fonction permet d'afficher la vidéo à l'aide d'ExoPlayer
    private fun displayVideo(){

        player = ExoPlayer.Builder(requireContext()).build()

        // On associe l'ExoPlayer au playerView dans le layout
        binding.playerView.player = player

        // On récupère un MediaItem à partir de l'Uri de notre vidéo
        val mediaItem = MediaItem.fromUri(StorageFragment.videoList[position].artUri)

        // On met le mode d'affichage en loop
        player.repeatMode = Player.REPEAT_MODE_ONE

        // Lie la vidéo au ExoPlayer et lance la vidéo
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

    }

    // On utilise cette fonction pour terminer la vidéo lorsque l'on quitte la page
    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
    }

    // Fonction qui est appellé à chaque nouvelle image extraite de la vidéo. On l'utilise pour effectuer notre Pose Detection sur la frame
    override fun onCurrentFrameExtracted(currentFrame: Frame) {
        val startSavingTime = System.currentTimeMillis()

        // On convertit l'image en bitMap pour pouvoir l'utiliser ensuite
        var imageBitmap = Utils.fromBufferToBitmap(currentFrame.byteBuffer, currentFrame.width, currentFrame.height)

        // Vérification de la position de l'image en rotation et vérification d'une possible inversion horizontale ou verticale
        if (imageBitmap != null ) {
            if(currentFrame.rotation == 180){
                imageBitmap = RotateBitmap(imageBitmap,currentFrame.rotation.toFloat())
            }
            if(currentFrame.isFlipX || currentFrame.isFlipY){
                imageBitmap = createFlippedBitmap(imageBitmap!!,currentFrame.isFlipX,currentFrame.isFlipY)
            }
        }


        // On récupère le fichier de la frame actuelle
        val allFrameFileFolder = File(requireContext().getExternalFilesDir(null), UUID.randomUUID().toString())
        if (!allFrameFileFolder.isDirectory) {
            allFrameFileFolder.mkdirs()
        }
        val frameFile = File(allFrameFileFolder, "frame_num_${currentFrame.timestamp.toString().padStart(10, '0')}.jpeg")

        // On sauvegarde temporairement l'image
        imageBitmap?.let {
            val savedFile = Utils.saveImageToFile(it, frameFile)
            savedFile?.let {
                imagePaths.add(savedFile.toUri())
                titles.add("${currentFrame.position} (${currentFrame.timestamp})")
            }
        }


        // Processus de détection de MLKit
        if (imageBitmap != null) {
            val processImage = InputImage.fromBitmap(imageBitmap, 0)
            poseDetector.process(processImage)
                .addOnSuccessListener {
                    // On récupère tous les points qui nous intéressent
                    val leftShoulderX = it.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position3D?.x?.div(imageBitmap.width)
                    val leftShoulderY = it.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)?.position3D?.y?.div(imageBitmap.height)
                    val rightShoulderX = it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.x?.div(imageBitmap.width)
                    val rightShoulderY = it.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)?.position3D?.y?.div(imageBitmap.height)
                    val leftElbowX = it.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.position3D?.x?.div(imageBitmap.width)
                    val leftElbowY = it.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.position3D?.y?.div(imageBitmap.height)
                    val rightElbowX = it.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.position3D?.x?.div(imageBitmap.width)
                    val rightElbowY = it.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.position3D?.y?.div(imageBitmap.height)
                    val leftWristX = it.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.position3D?.x?.div(imageBitmap.width)
                    val leftWristY = it.getPoseLandmark(PoseLandmark.LEFT_WRIST)?.position3D?.y?.div(imageBitmap.height)
                    val rightWristX = it.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.position3D?.x?.div(imageBitmap.width)
                    val rightWristY = it.getPoseLandmark(PoseLandmark.RIGHT_WRIST)?.position3D?.y?.div(imageBitmap.height)
                    val leftHipX = it.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position3D?.x?.div(imageBitmap.width)
                    val leftHipY = it.getPoseLandmark(PoseLandmark.LEFT_HIP)?.position3D?.y?.div(imageBitmap.height)
                    val rightHipX = it.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position3D?.x?.div(imageBitmap.width)
                    val rightHipY = it.getPoseLandmark(PoseLandmark.RIGHT_HIP)?.position3D?.y?.div(imageBitmap.height)
                    val leftKneeX = it.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.position3D?.x?.div(imageBitmap.width)
                    val leftKneeY = it.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.position3D?.y?.div(imageBitmap.height)
                    val rightKneeX = it.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.position3D?.x?.div(imageBitmap.width)
                    val rightKneeY = it.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.position3D?.y?.div(imageBitmap.height)
                    val leftAnkleX = it.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.position3D?.x?.div(imageBitmap.width)
                    val leftAnkleY = it.getPoseLandmark(PoseLandmark.LEFT_ANKLE)?.position3D?.y?.div(imageBitmap.height)
                    val rightAnkleX = it.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.position3D?.x?.div(imageBitmap.width)
                    val rightAnkleY = it.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)?.position3D?.y?.div(imageBitmap.height)

                    // On charge le modèle pour détecter le mouvement
                    val model = Model.newInstance(requireContext())

                    // On crée un buffer pour y mettre les données utiles
                    val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 24), DataType.FLOAT32)
                    var byteBuffer : ByteBuffer = ByteBuffer.allocateDirect(4*24)
                    byteBuffer.order(ByteOrder.nativeOrder())

                    // On remplit le byteBuffer
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
                }
        }

        totalSavingTimeMS += System.currentTimeMillis() - startSavingTime

        // Affiche sur l'écran le nombre de frame extraite
        requireActivity().runOnUiThread {
            binding.infoTextView.text = "Extract ${currentFrame.position} frames"
        }
    }

    // Quand toutes les frames sont extraites on affiche le résultat à l'aide de la fonction chooseMovement() et on modifie l'aspect de l'interface
    @SuppressLint("SetTextI18n")
    override fun onAllFrameExtracted(processedFrameCount: Int, processedTimeMs: Long) {
        binding.infoTextView.text = "Extract $processedFrameCount frames took $processedTimeMs ms| Saving took: $totalSavingTimeMS ms"

        binding.infoTextView.visibility = INVISIBLE
        chooseMovement()
        movementTab = arrayOf(0,0,0)
        binding.buttonTest.isClickable = true
        binding.resultLayout.visibility = VISIBLE
    }


    // Cette fonction nous permet d'effectuer une rotation sur l'image
    private fun RotateBitmap(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    // Cette fonction nous permet d'effectuer un flip sur l'image
    private fun createFlippedBitmap(source: Bitmap, xFlip: Boolean, yFlip: Boolean): Bitmap? {
        val matrix = Matrix()
        matrix.postScale((if (xFlip) -1 else 1).toFloat(), (if (yFlip) -1 else 1).toFloat(), source.width / 2f, source.height / 2f)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    // Cette fonction nous permet de choisir entre les mouvements à la fin du post-process
    private fun chooseMovement(){
        if(movementTab[0] > 5 || movementTab[1] > 5 || movementTab[2] > 5){
            if (movementTab[0] > movementTab[1] && movementTab[0] > movementTab[2]){
                binding.exoName.text = getString(R.string.deadlift_name)
            } else if (movementTab[1] > movementTab[0] && movementTab[1] > movementTab[2]){
                binding.exoName.text = getString(R.string.squat_name)
            }else{
                binding.exoName.text = getString(R.string.bench_name)
            }
        } else {
            binding.exoName.text = getString(R.string.no_mouvement)
        }
    }
}