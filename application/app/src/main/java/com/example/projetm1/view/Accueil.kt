package com.example.projetm1.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.example.projetm1.databinding.FragmentAccueilBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class Accueil : Fragment() {
    private lateinit var binding : FragmentAccueilBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccueilBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentLayout = binding.parentLayout

        parentLayout.post {
            var buttonWidth = binding.parentLayout.width.div(3)
            Log.d("accueil", "buttonWidth = $buttonWidth    " + binding.parentLayout.width)
            binding.goToLivePreviewButton.width = buttonWidth
            binding.goToRecodingButton.width = buttonWidth
            binding.rulesButton.width = buttonWidth
            binding.goToGalleryButton.width = buttonWidth



            binding.goToLivePreviewButton.setOnClickListener {
                val action = AccueilDirections.actionAccueilToLiveFragment()
                it.findNavController().navigate(action)
            }

            binding.goToGalleryButton.setOnClickListener {
                val action = AccueilDirections.actionAccueilToStorageFragment()
                it.findNavController().navigate(action)
            }

            binding.rulesButton.setOnClickListener {
                val action = AccueilDirections.actionAccueilToRulesFragment()
                it.findNavController().navigate(action)
            }

            binding.goToRecodingButton.setOnClickListener {
                val action = AccueilDirections.actionAccueilToRecordFragment()
                it.findNavController().navigate(action)
            }

        }
    }


}