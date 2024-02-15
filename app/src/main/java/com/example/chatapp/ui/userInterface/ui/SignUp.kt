package com.example.chatapp.ui.userInterface.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentSignUpBinding
import com.example.chatapp.ui.userInterface.ui.MainActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class SignUp : Fragment(R.layout.fragment_sign_up) {
    private lateinit var binding: FragmentSignUpBinding
    private lateinit var navController: NavController
    var Auth: FirebaseAuth? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignUpBinding.bind(view)
        navController = Navigation.findNavController(view)

        val activity = activity as MainActivity
        activity.supportActionBar?.hide()

        //to create object
        Auth = FirebaseAuth.getInstance()
        //for sign up
        binding.register.setOnClickListener() {
            signUp()
        }
        binding.loginTest.setOnClickListener()
        {
            var bundle= bundleOf("userName" to " ","userCase" to "Login")
            navController.navigate(R.id.action_signUp_to_signIn,bundle)
        }
    }
    //******************************************************************************
    private fun signUp() {
        //for register with email and password
        if (binding.email.text.isNotEmpty() && binding.password.text.isNotEmpty()&&binding.username.text.isNotEmpty()) {
            binding.reEmail.isVisible = false
            binding.rePassword.isVisible = false
            binding.reusername.isVisible = false
            binding.progressBar.isVisible = true
            Auth?.createUserWithEmailAndPassword(
                binding.email.text.toString(),
                binding.password.text.toString()
            )
                ?.addOnCompleteListener(object : OnCompleteListener<AuthResult> {
                    override fun onComplete(p0: Task<AuthResult>) {
                        if (p0.isSuccessful) {
                            //email and password are written correctly
                            sendEmailVerification()
                            binding.progressBar.isVisible = false
                        } else {
                            // email or password is not written correctly
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
                binding.reEmail.isVisible = true
            if (binding.password.text.isEmpty())
                binding.rePassword.isVisible = true
            if (binding.username.text.isEmpty())
                binding.reusername.isVisible = true
        }
    }

    //******************************************************************************
    private fun sendEmailVerification() {
        //send message to verify email
        Auth?.currentUser?.sendEmailVerification()?.addOnCompleteListener {
            if (it.isSuccessful) {
                //Message has been sent successfully
                Toast.makeText(requireContext(), "Successful", Toast.LENGTH_LONG)
                    .show()
                //move to login page to verify email
                var bundle= bundleOf("userName" to binding.username.text.toString(),"userCase" to "Register")
                navController.navigate(R.id.action_signUp_to_signIn,bundle)
            } else {
                //there is problem in sending
                Toast.makeText(
                    requireContext(),
                    it.exception.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}