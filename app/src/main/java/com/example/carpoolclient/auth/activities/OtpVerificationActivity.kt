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
import com.example.carpoolclient.auth.services.AuthService
import com.example.carpoolclient.utils.LoadingDialog

class OtpVerificationActivity : AppCompatActivity() {
    private val authService = AuthService()
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_otp_verification)

        loadingDialog = LoadingDialog(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + ime.bottom)
            insets
        }

        val etOtp = findViewById<EditText>(R.id.et_otp)
        val btnVerifyOtp = findViewById<Button>(R.id.btn_verify_otp)
        val tvMessage = findViewById<TextView>(R.id.tv_otp_message)
        val scrollView = findViewById<android.widget.ScrollView>(R.id.scroll_view)

        etOtp.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.postDelayed({
                    scrollView.smoothScrollTo(0, btnVerifyOtp.bottom + 100)
                }, 100)
            }
        }

        val email = intent.getStringExtra("EMAIL") ?: ""
        if (email.isNotEmpty()) {
            tvMessage.text = getString(R.string.otp_sent_message, email)
        }

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()

            if (otp.length == 3) {
                btnVerifyOtp.isEnabled = false
                loadingDialog.show()
                authService.authenticateUser(otp, email) { success, message ->
                    runOnUiThread {
                        btnVerifyOtp.isEnabled = true
                        loadingDialog.dismiss()
                        if (success) {
                            Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, RegisterActivity::class.java)
                            intent.putExtra("EMAIL", email)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Verification failed: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                etOtp.error = "Please enter the 3-digit code"
            }
        }
    }
}