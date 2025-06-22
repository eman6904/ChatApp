package com.example.chatapp.ui.userInterface.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.chatapp.ui.userInterface.ui.fragments.Home
import com.example.chatapp.ui.userInterface.ui.fragments.Users

class TabsAdapter (fragmentActivity: Home) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return  when(position){
            0 -> Users()
            1 -> Users()
            else -> Users()
        }
    }

}