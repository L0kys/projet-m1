package com.example.projetm1.view

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projetm1.Video
import com.example.projetm1.databinding.StorageFragmentBinding
import java.io.File


/**
 * StorageFragment est la page qui donne l'accès aux vidéos à l'utilisateur pour effectuer du post-process. C'est une page avec un recyclerView qui affiche
 * toute les vidéos prises depuis la page RecordFragment. Lorsque l'utilisateur effectue un click sur une des vidéos la position de la vidéo (dans le recyclerView)
 * est envoyé à PlayerFragment.
 */


class StorageFragment: Fragment() {

    private lateinit var binding: StorageFragmentBinding
    companion object {
        lateinit var videoList: ArrayList<Video>
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = StorageFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated (view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoList = getAllVideos(requireContext())
        binding.videoListRecyclerView.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            layoutManager = LinearLayoutManager(context)
            adapter = VideoListAdapter(context, videoList)
        }
    }


    // Cette fonction récupère toutes les vidéos du stockage du téléphone à l'aide d'une query. On trie les vidéos pour garder que celle qui sont dans le dossier ReferAI
    @SuppressLint("InlinedApi", "Recycle", "Range")
    fun getAllVideos(context: Context): ArrayList<Video> {
        val tempList = ArrayList<Video>()

        // Filtre des vidéos pour garder celle qui sont dans le dossier ReferAI
        val selection = MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " LIKE 'ReferAI'"
        val projection = arrayOf(
            MediaStore.Video.Media.TITLE, MediaStore.Video.Media.SIZE, MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION
        )
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, null, MediaStore.Video.Media.DATE_ADDED + " DESC"
        )

        // Récupère les informations de chaque vidéo
        cursor?.use {
            while (cursor.moveToNext()) {
                val titleC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)) ?: "Unknown"
                val idC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)) ?: "Unknown"
                val folderC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)) ?: "Internal Storage"
                val sizeC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)) ?: "0"
                val pathC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) ?: "Unknown"
                val durationC = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))?.toLongOrNull() ?: 0L
                Log.d("folder", "$cursor $folderC")

                try {
                    val file = File(pathC)
                    val artUriC = Uri.fromFile(file)
                    // Ajoute toute les infos dans l'objet Video
                    val video = Video(
                        title = titleC,
                        id = idC,
                        folderName = folderC,
                        duration = durationC,
                        size = sizeC,
                        path = pathC,
                        artUri = artUriC
                    )
                    if (file.exists()) {
                        tempList.add(video)
                    }

                } catch (e: Exception) {
                    // Handle the exception if necessary
                }
            }
        }
        return tempList
    }
}

