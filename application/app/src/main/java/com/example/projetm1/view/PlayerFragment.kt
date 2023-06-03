package com.example.projetm1.view


import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.projetm1.Utils
import com.example.projetm1.databinding.PlayerFragmentBinding
import com.example.projetm1.decoder.Frame
import com.example.projetm1.decoder.FrameExtractor
import com.example.projetm1.decoder.IVideoFrameExtractor
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.ArrayList
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class PlayerFragment: Fragment(), IVideoFrameExtractor {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val poseDetector = PoseDetection.getClient(options)

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

    override fun onViewCreated (view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayVideo()
        binding.buttonTest.setOnClickListener{
            postProcess()
        }
    }

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

    private fun displayVideo(){
        player = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = player


        val mediaItem = MediaItem.fromUri(StorageFragment.videoList[position].artUri)
        Log.d("quoicoubeh", StorageFragment.videoList[position].artUri.toString())
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onCurrentFrameExtracted(currentFrame: Frame) {
        val startSavingTime = System.currentTimeMillis()
        // 1. Convert frame byte buffer to bitmap
        val imageBitmap = Utils.fromBufferToBitmap(currentFrame.byteBuffer, currentFrame.width, currentFrame.height)

        // 2. Get the frame file in app external file directory
        val allFrameFileFolder = File(requireContext().getExternalFilesDir(null), UUID.randomUUID().toString())
        if (!allFrameFileFolder.isDirectory) {
            allFrameFileFolder.mkdirs()
        }
        val frameFile = File(allFrameFileFolder, "frame_num_${currentFrame.timestamp.toString().padStart(10, '0')}.jpeg")

        // 3. Save current frame to storage
        imageBitmap?.let {
            val savedFile = Utils.saveImageToFile(it, frameFile)
            savedFile?.let {
                imagePaths.add(savedFile.toUri())
                titles.add("${currentFrame.position} (${currentFrame.timestamp})")
            }
        }

        totalSavingTimeMS += System.currentTimeMillis() - startSavingTime

        requireActivity().runOnUiThread {
            binding.infoTextView.text = "Extract ${currentFrame.position} frames"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onAllFrameExtracted(processedFrameCount: Int, processedTimeMs: Long) {
        binding.infoTextView.text = "Extract $processedFrameCount frames took $processedTimeMs ms| Saving took: $totalSavingTimeMS ms"
    }
}