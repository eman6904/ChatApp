package com.example.chatapp.ui.userInterface.ui

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.databinding.FragmentProfileBinding
import com.example.chatapp.ui.userInterface.model.UserItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*


class Profile : Fragment(R.layout.fragment_profile) {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var navController: NavController
    var obj: DatabaseReference? = null
    var storage: StorageReference? = null
    var uriImage: Uri? = null
    var profileImageUri:String=""
    var usernameSize = 20
    var descrSize=50
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)
        navController = Navigation.findNavController(view)

        val activity = activity as MainActivity
        activity.supportActionBar?.hide()

        storage = FirebaseStorage.getInstance().reference
        var currentUser = FirebaseAuth.getInstance()?.currentUser!!
        var id = currentUser.uid

        obj = FirebaseDatabase.getInstance().getReference("User").child(id)

        obj?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserItems::class.java)
                if (user?.username != "")
                    binding.username.setText(user!!.username)
                if (user?.descr != "")
                    binding.description.setText(user!!.descr)
                Glide.with(activity.applicationContext).asBitmap().load(Uri.parse(user!!.profilePhoto))
                    .placeholder(R.drawable.personalphotojpg).into(binding.profilephoto)

            }
        })
        /////////////////////////////////////////////////////////////////////////////////
        binding.updatUsername.setOnClickListener()
        {
            val alertbuilder = AlertDialog.Builder(requireContext())
            val view = layoutInflater.inflate(R.layout.update, null)
            alertbuilder.setView(view)
            val alertDialog = alertbuilder.create()
            alertDialog.show()

            val counter = view.findViewById<TextView>(R.id.counter)
            val edText = view.findViewById<EditText>(R.id.edtext)
            val setButton = view.findViewById<ImageView>(R.id.set)

            //////////////////////////////////////////////////
            var username=binding.username.text.toString()
            edText.setText(username)
            counter.setText((usernameSize-username.length).toString())
            val mTextEditorWatcher: TextWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    //This sets a textview to the current length
                    if(s.length<=19)
                     counter.setText((usernameSize-s.length).toString())
                    else {
                        counter.setText((usernameSize - s.length).toString())
                        edText.isEnabled = false
                    }
                }

                override fun afterTextChanged(s: Editable) {}
            }
            edText.addTextChangedListener(mTextEditorWatcher)

            setButton.setOnClickListener()
            {
                val hashMap: HashMap<String, Any> = HashMap()
                hashMap.put("username", edText.text.toString())
                obj?.updateChildren(hashMap as Map<String, Any>)?.addOnSuccessListener {
                    Toast.makeText(view!!.context, "Successful", Toast.LENGTH_LONG).show()
                }?.addOnFailureListener {
                    Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
                }
                alertDialog.dismiss()
            }
        }
        /////////////////////////////////////////////////////////////////////////////////
        binding.updatDesc.setOnClickListener()
        {
            val alertbuilder = AlertDialog.Builder(requireContext())
            val view = layoutInflater.inflate(R.layout.update, null)
            alertbuilder.setView(view)
            val alertDialog = alertbuilder.create()
            alertDialog.show()

            val counter = view.findViewById<TextView>(R.id.counter)
            val edText = view.findViewById<EditText>(R.id.edtext)
            val setButton = view.findViewById<ImageView>(R.id.set)

            //////////////////////////////////////////////////
            var desc=binding.description.text.toString()
            edText.setText(desc)
            counter.setText((descrSize-desc.length).toString())
            val mTextEditorWatcher: TextWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    //This sets a textview to the current length
                    if(s.length<=49)
                        counter.setText((descrSize-s.length).toString())
                    else {
                        counter.setText((descrSize - s.length).toString())
                        edText.isEnabled = false
                    }
                }

                override fun afterTextChanged(s: Editable) {}
            }
            edText.addTextChangedListener(mTextEditorWatcher)

            setButton.setOnClickListener()
            {
                val hashMap: HashMap<String, Any> = HashMap()
                hashMap.put("descr", edText.text.toString())
                obj?.updateChildren(hashMap as Map<String, Any>)?.addOnSuccessListener {
                    Toast.makeText(view!!.context, "Successful", Toast.LENGTH_LONG).show()
                }?.addOnFailureListener {
                    Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
                }
                alertDialog.dismiss()
            }
        }
        /////////////////////////////////////////////////////////////////////////////////
        binding.updatePhoto.setOnClickListener()
        {
            val alertbuilder = AlertDialog.Builder(requireContext())
            val view = layoutInflater.inflate(R.layout.updatephoto, null)
            alertbuilder.setView(view)
            val alertDialog = alertbuilder.create()
            alertDialog.show()
            val setphoto = view.findViewById<TextView>(R.id.set_photo)
            val unsetphoto = view.findViewById<TextView>(R.id.unset_photo)
            setphoto.setOnClickListener()
            {
                val intentImage = Intent(ACTION_PICK)
                intentImage.type = "image/*"
                startActivityForResult(intentImage, 2)
                alertDialog.dismiss()
            }
            unsetphoto.setOnClickListener()
            {
                unsetPhoto()
                alertDialog.dismiss()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == RESULT_OK) {
            uriImage = data!!.data
            storage?.child("image/" + UUID.randomUUID().toString())?.putFile(uriImage!!)?.addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    profileImageUri = uri.toString()
                    Log.d("profillllle",profileImageUri)
                    setPhoto()
            }
                Toast.makeText(requireContext(),"Uploaded",Toast.LENGTH_LONG).show()

            }?.addOnFailureListener(){
                Toast.makeText(requireContext(),it.message,Toast.LENGTH_LONG).show()
            }
            setPhoto()
        }

    }

    private fun setPhoto() {
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap.put("profilePhoto", profileImageUri)
        obj?.updateChildren(hashMap as Map<String, Any>)?.addOnFailureListener {
            Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun unsetPhoto() {
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap.put("profilePhoto", "")
        obj?.updateChildren(hashMap as Map<String, Any>)?.addOnFailureListener {
            Toast.makeText(view!!.context, it.message, Toast.LENGTH_LONG).show()
        }
    }
// put these permissions in manifest
//    <uses-permission android:name="android.permission.INTERNET"></uses-permission>
//    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"></uses-permission>
}