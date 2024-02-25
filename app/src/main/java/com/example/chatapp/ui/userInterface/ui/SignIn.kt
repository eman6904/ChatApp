package com.example.chatapp.ui.userInterface.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentSignInBinding
import com.example.chatapp.ui.userInterface.model.UserItems
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignIn : Fragment(R.layout.fragment_sign_in) {
    private lateinit var binding: FragmentSignInBinding
    private lateinit var navController: NavController
    var Auth: FirebaseAuth? = null
    var obj: DatabaseReference? = null
    var username:String=""
    var userCase:String=""
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignInBinding.bind(view)
        navController = Navigation.findNavController(view)

        val activity = activity as MainActivity
        activity.supportActionBar?.hide()

        username= arguments?.getString("userName").toString()
        userCase= arguments?.getString("userCase").toString()

        //to create object
        Auth = FirebaseAuth.getInstance()
        //for login
        binding.login.setOnClickListener()
        {
            signIn()
        }
    }
    //***************************************************************************************
    private fun signIn() {
        if (binding.email.text.isNotEmpty() && binding.password.text.isNotEmpty()) {
            binding.reemail.isVisible = false
            binding.repassword.isVisible = false
            binding.progressBar.isVisible = true
            Auth?.signInWithEmailAndPassword(
                binding.email.text.toString(),
                binding.password.text.toString()
            )
                ?.addOnCompleteListener(object : OnCompleteListener<AuthResult> {
                    override fun onComplete(p0: Task<AuthResult>) {
                        if (p0.isSuccessful) {
                            //This account already exists
                            verifyEmailAddress()
                            binding.progressBar.isVisible = false
                        } else {
                            //this account is not found or there is error
                            Toast.makeText(
                                requireContext(),
                                p0.exception.toString(),
                                Toast.LENGTH_LONG
                            ).show()
                            binding.progressBar.isVisible = false
                        }
                    }
                })
        } else {
            if (binding.email.text.isEmpty())
                binding.reemail.isVisible = true
            if (binding.password.text.isEmpty())
                binding.repassword.isVisible = true
        }
    }
    //*********************************************************************************
    private fun verifyEmailAddress()
    {
        //for verify email
        if(Auth?.currentUser!!.isEmailVerified)
        {
            Toast.makeText(requireContext(), "Successful", Toast.LENGTH_LONG)
                .show()
            if(userCase=="Register")
                addUser()
            navController.navigate(R.id.action_signIn_to_users)
        }else{
            addUser()
            Toast.makeText(requireContext(), "Please verify your account..", Toast.LENGTH_LONG).show()
        }
    }
    private fun addUser()
    {
        obj = FirebaseDatabase.getInstance().getReference("User")
        var currentUserId = FirebaseAuth.getInstance()?.currentUser!!.uid
        var user= UserItems(currentUserId,username,"","","","")
        obj?.child(currentUserId)?.setValue(user)
    }
}