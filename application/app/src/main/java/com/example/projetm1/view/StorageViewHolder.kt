package com.example.projetm1.view

import androidx.recyclerview.widget.RecyclerView
import com.example.projetm1.databinding.ViewHolderVideoBinding


class StorageViewHolder (binding: ViewHolderVideoBinding) : RecyclerView.ViewHolder(binding.root){
    val title = binding.videoName
    val duration = binding.duration
    val image = binding.videoImg
    val root = binding.root
}