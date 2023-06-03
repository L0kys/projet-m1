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

/**
 * Ce fragment est destiné à être la première vue que l'utilisateur verra en ouvrant l'application.
 * Il est central dans la navigation et donne accès à quatres boutons qui renvoient vers les fragments
 * spécialisés pour chaque fonctionnalité que nous avons implémenté
 * **/

@AndroidEntryPoint
class Accueil : Fragment() {
    private lateinit var binding : FragmentAccueilBinding

    // Création du fragment et du binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccueilBinding.inflate(inflater)
        return binding.root
    }

    //Fonction s'effectuant juste après onCreateView qui va nous permettre de mettre en place les listener sur les bouttons
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parentLayout = binding.parentLayout

        // Ici on attend que le layout principal soit mis en place pour pouvoir récupérer sa largeur
        // Puis on la divise par trois et on l'applique aux bouttons ce qui permet de s'assurer qu'ils aient la même taille
        parentLayout.post {
            var buttonWidth = binding.parentLayout.width.div(3)
            binding.goToLivePreviewButton.width = buttonWidth
            binding.goToRecodingButton.width = buttonWidth
            binding.rulesButton.width = buttonWidth
            binding.goToGalleryButton.width = buttonWidth


            // Mise en place des listener qui permettent la navigation après que les bouttons soient préssés
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