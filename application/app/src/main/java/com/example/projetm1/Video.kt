package com.example.projetm1

import android.net.Uri


/**
 * Object Video, qui contient toutes les informations utiles pour une vidéo
 */


data class Video(
    val id: String,
    val title: String,
    val duration: Long=0,
    val folderName: String,
    val size: String,
    val path: String,
    val artUri: Uri
    )
