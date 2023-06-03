package com.example.projetm1.view

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import com.example.projetm1.R
import com.example.projetm1.databinding.RecordFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * RecordFragment est la page qui nous sert à l'enregistrement de vidéo pour le post-process. Lors du click sur le bouton, l'enregistrement commence
 * à l'aide de la fonction captureVideo(). Pour arrêter l'enregistrement il suffit de réappuyer sur le bouton une seconde fois. La vidéo est ensuite enregistrée
 * dans un dossier "ReferAI" dans la galerie du téléphone.
 */


@AndroidEntryPoint
class RecordFragment: Fragment() {

    private lateinit var binding: RecordFragmentBinding

    private val FILENAMEFORMAT = "yyyy-MM-dd-HH-mm-ss"
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        binding = RecordFragmentBinding.inflate(inflater)
        return binding.root
    }

    @ExperimentalGetImage
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val previewView = binding.viewFinder

        // Bouton pour démarrer l'enregistrement
        binding.videoCaptureButton.setOnClickListener {
            captureVideo()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        //On démarre la caméra quand l'élément preview est chargé
        previewView.post{
            startCamera()
        }

    }

    // StartCamera allume la camera et crée les objets preview et enregistrement pour ensuite les lier à la caméra
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Utilisé pour lier les différents cas d'utilisation à la caméra
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Crée un objet Preview pour avoir un apercu de la caméra
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Crée un objet Record pour permettre à la caméra d'enregistrer
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Sélectionne la caméra arrière par défaut
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Détache tous les cas d'utilisation avant de les rattacher
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture)

            } catch(exc: Exception) {

            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Permets de lancer l'enregistrement d'une vidéo
    private fun captureVideo() {

        val videoCapture = this.videoCapture ?: return
        val curRecording = recording
        binding.videoCaptureButton.isEnabled = false

        // Si on enregistre on arrête l'enregistrement (cas du deuxième click sur le bouton)
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }

        // On prépare les métadonnées pour le fichier vidéo qui sera enregistré
        val name = SimpleDateFormat(FILENAMEFORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.TAGS, "ReferAI")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ReferAI")
            }
        }

        val resolver = requireActivity().contentResolver

        // On structure les métadonnées dans un MediaStoreOutputOptions
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = context?.let {
            videoCapture.output
                .prepareRecording(it, mediaStoreOutputOptions)
                .apply {
                    if (PermissionChecker.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    when(recordEvent) {
                        is VideoRecordEvent.Start -> {
                            // Début de l'enregistrement, on change l'aspect du bouton
                            binding.videoCaptureButton.apply {
                                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_stop_24))
                                isEnabled = true
                            }
                        }

                        is VideoRecordEvent.Finalize -> {
                            // Fin de l'enregistrement, on met le bouton par défaut
                            if (!recordEvent.hasError()) {
                                val msg = "Video enregistrée"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                recording?.close()
                                recording = null
                            }

                            binding.videoCaptureButton.apply {
                                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_videocam_24))
                                isEnabled = true
                            }
                        }
                    }
                }
        }
    }

}