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
import com.example.carpoolclient.MainActivity
import com.example.carpoolclient.R
import com.example.carpoolclient.auth.dtos.RegisterRequest
import com.example.carpoolclient.auth.services.AuthService
import com.example.carpoolclient.utils.LoadingDialog

class RegisterActivity : AppCompatActivity() {
    private val authService = AuthService()
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        loadingDialog = LoadingDialog(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom + ime.bottom)
            insets
        }

        val etFirstName = findViewById<EditText>(R.id.et_first_name)
        val etLastName = findViewById<EditText>(R.id.et_last_name)
        val etPhoneNumber = findViewById<EditText>(R.id.et_phone_number)
        val etStudentId = findViewById<EditText>(R.id.et_student_id)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnFinish = findViewById<Button>(R.id.btn_complete_registration)
        val scrollView = findViewById<android.widget.ScrollView>(R.id.scroll_view)

        val email = intent.getStringExtra("EMAIL") ?: ""

        val editTexts = listOf(etFirstName, etLastName, etPhoneNumber, etStudentId, etPassword, etConfirmPassword)
        editTexts.forEach { et ->
            et.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    scrollView.postDelayed({
                        scrollView.smoothScrollTo(0, et.bottom + 100)
                    }, 100)
                }
            }
        }

        btnFinish.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val phoneNumber = etPhoneNumber.text.toString().trim()
            val studentId = etStudentId.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty() || studentId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val registerRequest = RegisterRequest(
                firstName,
                lastName,
                email,
                password,
                phoneNumber,
                studentId
            )

            btnFinish.isEnabled = false
            loadingDialog.show()
            authService.registerUser(registerRequest) { success, message ->
                runOnUiThread {
                    btnFinish.isEnabled = true
                    loadingDialog.dismiss()
                    if (success) {
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val displayMessage = if (message?.contains("user exists", ignoreCase = true) == true) {
                            "Account already registered"
                        } else {
                            message
                        }
                        Toast.makeText(this, "Registration failed: $displayMessage", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}