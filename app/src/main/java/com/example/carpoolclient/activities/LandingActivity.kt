package com.example.carpoolclient.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.carpoolclient.R

class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        findViewById<Button>(R.id.btn_get_started).setOnClickListener {
            // Navigate to EmailVerification when Get Started is clicked
            startActivity(Intent(this, EmailVerification::class.java))
        }
    }
}