package com.example.HealthyMode.UI.Auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.HealthyMode.R
import com.example.HealthyMode.UI.Home.Home_screen
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class Login_fragment : Fragment() {

    private lateinit var fAuth: FirebaseAuth
    private var btnEnable = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_login_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fAuth = FirebaseAuth.getInstance()

        // Setup Animations on screen load
        setupAnimations(view)

        val logButton: Button = view.findViewById(R.id.loginbtn)
        logButton.setOnClickListener {
            if (btnEnable) {
                btnEnable = false
                createUserLog(view)

                // Prevent double-clicking
                Handler(Looper.getMainLooper()).postDelayed({
                    btnEnable = true
                }, 1000)
            }
        }

        setupForgotPassword(view)
    }

    private fun setupAnimations(view: View) {
        val passError: TextView = view.findViewById(R.id.password_error)
        val layEmail: TextInputLayout = view.findViewById(R.id.tvemail)
        val layPass: TextInputLayout = view.findViewById(R.id.tvpass)

        val rightAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.rightt_left)
        val leftAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.left_right)

        passError.animation = leftAnimation
        layEmail.animation = rightAnimation
        layPass.animation = leftAnimation
    }

    private fun createUserLog(view: View) {
        val mEmail: EditText = view.findViewById(R.id.email)
        val mPassword: EditText = view.findViewById(R.id.password)
        val passError: TextView = view.findViewById(R.id.password_error)

        val emailText = mEmail.text.toString().trim()
        val passwordText = mPassword.text.toString().trim()

        if (emailText.isEmpty()) {
            mEmail.error = "Email is Required."
            return
        }
        if (passwordText.isEmpty()) {
            passError.text = "Password is Required *"
            return
        }
        if (passwordText.length < 6) {
            passError.text = "Password Must be greater than 6 Characters *"
            return
        }

        // Optional: Show your progress bar here

        fAuth.signInWithEmailAndPassword(emailText, passwordText).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = fAuth.currentUser
//                if (user != null && user.isEmailVerified) {
//
//
//                } else {
//                    Toast.makeText(view.context, "You did not verify your Email", Toast.LENGTH_LONG).show()
//
//                }
                Toast.makeText(view.context, "Logged in Successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), Home_screen::class.java))
                requireActivity().finish()
            } else {
                Toast.makeText(
                    view.context,
                    "Error! ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
                // Optional: Hide progress bar here
            }
        }
    }

    private fun setupForgotPassword(view: View) {
        val forgotTextLink: TextView = view.findViewById(R.id.forgotPassword)
        forgotTextLink.setOnClickListener { v ->
            val resetMail = EditText(v.context)
            val passwordResetDialog = AlertDialog.Builder(v.context)
                .setTitle("Reset Password?")
                .setMessage("Enter Your Email To Receive a Reset Link.")
                .setView(resetMail)
                .setPositiveButton("Yes") { _, _ ->
                    val mail = resetMail.text.toString().trim()
                    if (mail.isEmpty()) {
                        Toast.makeText(v.context, "Error: Email is Required.", Toast.LENGTH_SHORT).show()
                        // Note: Dialog will still close here. To prevent this, you'd need to override the button click listener after dialog.show()
                    } else {
                        fAuth.sendPasswordResetEmail(mail).addOnSuccessListener {
                            Toast.makeText(v.context, "Reset Link Sent To Your Email.", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener { e ->
                            Toast.makeText(v.context, "Error! Reset Link Not Sent: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("No", null)
                .create()

            passwordResetDialog.show()
        }
    }
}