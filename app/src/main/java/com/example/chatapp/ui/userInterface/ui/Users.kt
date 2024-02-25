package com.example.chatapp.ui.userInterface.ui

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.UsersBinding
import com.example.chatapp.ui.userInterface.model.UserAdapter
import com.example.chatapp.ui.userInterface.model.UserItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Users : Fragment(R.layout.users) {
    private lateinit var binding: UsersBinding
    private lateinit var navController: NavController
    var obj: DatabaseReference? = null
    var list=ArrayList<UserItems>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = UsersBinding.bind(view)
        navController = Navigation.findNavController(view)

        val activity = activity as MainActivity
        activity.supportActionBar?.hide()

        binding.progressBar.isVisible = true

        binding.arrowBack.setOnClickListener()
        {
            navController.navigate(R.id.action_users_to_profile3)
        }

    }

    //************************************************************
    override fun onStart() {
        super.onStart()
        obj = FirebaseDatabase.getInstance().getReference("User")
        //current user is the person who is currently registered or made login
        var currentUserId = FirebaseAuth.getInstance()?.currentUser!!.uid
//            //to create new id
//            var id = obj!!.push().key
        obj?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                for (data in snapshot.children) {
                    val user = data.getValue(UserItems::class.java)
                    if(!user!!.id.equals(currentUserId))
                        list.add(user)
                    else{
                        Glide.with(requireContext()).asBitmap().load(Uri.parse(user!!.profilePhoto))
                            .placeholder(R.drawable.personalphotojpg).into(binding.profileImage)
                    }
                }
                binding.progressBar.isVisible = false
                val adapter = UserAdapter(list)
                binding.recycler.layoutManager = LinearLayoutManager(requireContext())
                binding.recycler.adapter = adapter

            }
            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.isVisible = false
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}