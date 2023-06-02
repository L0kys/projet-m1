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

class RulesFragment : Fragment() {



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRulesBinding.inflate(inflater)
        return binding.root
    }


}