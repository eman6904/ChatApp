package com.example.chatapp.ui.userInterface.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentSignInBinding
import com.example.chatapp.ui.userInterface.ui.MainActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class SignIn : Fragment(R.layout.fragment_sign_in) {
    private lateinit var binding: FragmentSignInBinding
    private lateinit var navController: NavController
    var Auth: FirebaseAuth? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignInBinding.bind(view)
        navController = Navigation.findNavController(view)

        val activity = activity as MainActivity
        activity.supportActionBar?.hide()
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
        if (binding.email.text.isNotEmpty() && binding.password.text.isNotEmpty()&&binding.username.text.isNotEmpty()) {
            binding.reemail.isVisible = false
            binding.repassword.isVisible = false
            binding.reusername.isVisible = false
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
                            //this account is not found
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
            if(binding.username.text.isEmpty())
                binding.reusername.isVisible=true
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
            var bundle= bundleOf("username" to binding.username.text.toString())
            navController.navigate(R.id.action_signIn_to_users,bundle)
        }else{
            Toast.makeText(requireContext(), "Please verify your account..", Toast.LENGTH_LONG).show()
        }
    }
}