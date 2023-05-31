package com.example.projetm1.view

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.projetm1.Video
import com.example.projetm1.databinding.ViewHolderVideoBinding


class VideoListAdapter(private val context: Context, private var videoList: ArrayList<Video>) : RecyclerView.Adapter<StorageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageViewHolder {
        val binding = ViewHolderVideoBinding.inflate(LayoutInflater.from(context), parent,false)
        return StorageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    override fun onBindViewHolder(holder: StorageViewHolder, position: Int) {
        holder.title.text = videoList[position].title
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration/1000)
    }
}

