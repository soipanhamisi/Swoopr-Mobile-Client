package com.example.carpoolclient.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.carpoolclient.R

class EmailVerification : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_verification)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.et_email)
        val btnVerify = findViewById<Button>(R.id.btn_verify)

        btnVerify.setOnClickListener {
            val email = etEmail.text.toString().trim()
            
            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (isValidUsiuEmail(email)) {
                // Navigate to OtpVerificationActivity
                val intent = Intent(this, OtpVerificationActivity::class.java)
                intent.putExtra("EMAIL", email)
                startActivity(intent)
            } else {
                etEmail.error = "Please enter a valid @usiu.ac.ke email address"
            }
        }
    }

    private fun isValidUsiuEmail(email: String): Boolean {
        return email.endsWith("@usiu.ac.ke", ignoreCase = true)
    }
}