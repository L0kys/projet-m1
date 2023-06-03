package com.example.projetm1.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.projetm1.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * La MainActivity de cette applicatoin n'a que deux objectifs principaux :
 *      Demander les permissions à l'utilisateur
 *      être l'hôte des différents fragments qui constituent l'application
 * **/
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // Tout le code de cette page excpté le binding ne sert qu'à demander les permissions à l'utilisateur et à été repris depuis le tutoriel pour l'utilisation de CameraX de developer.android.com
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request camera permissions
        if (allPermissionsGranted()) {
            val binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                val binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).toTypedArray()
    }



}