package com.example.carpoolclient.auth.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.carpoolclient.R

class OtpVerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp_verification)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etOtp = findViewById<EditText>(R.id.et_otp)
        val btnVerifyOtp = findViewById<Button>(R.id.btn_verify_otp)
        val tvMessage = findViewById<TextView>(R.id.tv_otp_message)

        val email = intent.getStringExtra("EMAIL")
        if (email != null) {
            tvMessage.text = getString(R.string.otp_sent_message, email)
        }

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()

            if (otp.length == 3) {
                // Here you would normally verify the OTP with your backend
                Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show()
                
                // Navigate to RegisterActivity to complete profile
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                etOtp.error = "Please enter the 6-digit code"
            }
        }
    }
}