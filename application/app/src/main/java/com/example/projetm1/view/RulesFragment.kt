package com.example.projetm1.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.projetm1.R
import com.example.projetm1.databinding.FragmentAccueilBinding
import com.example.projetm1.databinding.FragmentRulesBinding

private lateinit var binding: FragmentRulesBinding


/**
 * Ce fragment permet un simple affichage des règles d'utilisation que l'utilisateur doit prendre en compte
 * pour avoir une expérience optimale avec l'application. Il contient aussi toutes les règles associées au sport
 * que l'application doit pouvoir détecter.
 * **/
class RulesFragment : Fragment() {


    // Création du Fragment avec le layout qui contient toute sa fonction
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRulesBinding.inflate(inflater)
        return binding.root
    }


}