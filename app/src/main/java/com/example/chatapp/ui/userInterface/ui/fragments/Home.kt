package com.example.chatapp.ui.userInterface.ui.fragments

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentHomeBinding
import com.example.chatapp.databinding.FragmentSignUpBinding
import com.example.chatapp.ui.userInterface.ui.adapter.TabsAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth


class Home : Fragment(R.layout.fragment_home) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var navController: NavController
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)
        navController = Navigation.findNavController(view)

        val activity = activity as MainActivity
        activity.supportActionBar?.hide()
        setTabs(
            binding = binding,
            fragment = this,
            context = requireContext()
        )
    }
    private fun setTabs(
        binding: FragmentHomeBinding,
        fragment: Home,
        context: Context
    ) {

        binding.viewPager.adapter = TabsAdapter(fragment)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->

            val textView = TextView(binding.root.context).apply {
                text = when (pos) {
                    0 -> "CHATS"
                    1 -> "STATUS"
                    else -> "CALLS"
                }

                textSize = 16f
                typeface = ResourcesCompat.getFont(context, R.font.robot)
                gravity = Gravity.CENTER
               // setPadding(16, 16, 16, 16)
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.iconsColor))
            }

            tab.customView = textView
        }.attach()
        val firstTab = binding.tabLayout.getTabAt(0)
        (firstTab?.customView as? TextView)?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.mainColor)
        )
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                (tab?.customView as? TextView)?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.mainColor)
                )
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                (tab?.customView as? TextView)?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.iconsColor)
                )
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })


    }
}