package com.example.chatapp.ui.userInterface.ui.fragments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.UsersBinding
import com.example.chatapp.ui.userInterface.ui.adapter.UserAdapter
import com.example.chatapp.ui.userInterface.ui.model.UserItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class Users : Fragment(R.layout.users) {
    private lateinit var binding: UsersBinding
    private lateinit var navController: NavController
    private lateinit var friendsListener: ValueEventListener

    var friendsObj: DatabaseReference? = null
    var list=ArrayList<UserItems>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = UsersBinding.bind(view)

        (parentFragment as? Home)?.let {
            navController = it.findNavController()
        }


        val activity = activity as MainActivity
        activity.supportActionBar?.hide()

        binding.progressBar.isVisible = true

    }

    //************************************************************
    override fun onStart() {
        super.onStart()
        friendsObj = FirebaseDatabase.getInstance().getReference("User")
        //current user is the person who is currently registered or made login
        var currentUserId = FirebaseAuth.getInstance()?.currentUser!!.uid
//            //to create new id
//            var id = obj!!.push().key
        friendsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                for (data in snapshot.children) {
                    val user = data.getValue(UserItems::class.java)
                    if(!user!!.id.equals(currentUserId))
                        list.add(user)
                }
                //to make sure that views is active
               if(isAdded){
                   binding.progressBar.isVisible = false
                   val adapter = UserAdapter(list)
                   binding.recycler.layoutManager = LinearLayoutManager(requireContext())
                   binding.recycler.adapter = adapter
               }

            }
            override fun onCancelled(error: DatabaseError) {
               if(isAdded){
                   binding.progressBar.isVisible = false
                   Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
               }
            }
        }
        friendsObj?.addValueEventListener(friendsListener)
    }
    override fun onStop() {
        super.onStop()
        friendsObj?.removeEventListener(friendsListener)
    }
}