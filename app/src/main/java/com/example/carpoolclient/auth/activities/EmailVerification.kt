package com.example.carpoolclient.auth.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.carpoolclient.R
import com.example.carpoolclient.auth.services.AuthService
import com.example.carpoolclient.utils.LoadingDialog

class EmailVerification : AppCompatActivity() {
    private val authService = AuthService()
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_verification)
        
        loadingDialog = LoadingDialog(this)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + ime.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.et_email)
        val btnVerify = findViewById<Button>(R.id.btn_verify)
        val scrollView = findViewById<android.widget.ScrollView>(R.id.scroll_view)

        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    scrollView.smoothScrollTo(0, btnVerify.bottom + 100)
                }, 100)
            }
        }

        btnVerify.setOnClickListener {
            val email = etEmail.text.toString().trim()
            
            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (isValidUsiuEmail(email)) {
                btnVerify.isEnabled = false
                loadingDialog.show()
                authService.getOtp(email) { success, message ->
                    runOnUiThread {
                        btnVerify.isEnabled = true
                        loadingDialog.dismiss()
                        if (success) {
                            val intent = Intent(this, OtpVerificationActivity::class.java)
                            intent.putExtra("EMAIL", email)
                            startActivity(intent)
                        } else {
                            val displayMessage = if (message?.contains("user exists", ignoreCase = true) == true) {
                                "Account already registered"
                            } else {
                                message
                            }
                            Toast.makeText(this, "Error: $displayMessage", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                etEmail.error = "Please enter a valid @usiu.ac.ke email address"
            }
        }
    }

    private fun isValidUsiuEmail(email: String): Boolean {
        return email.endsWith("@usiu.ac.ke", ignoreCase = true)
    }
}