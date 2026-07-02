package com.example.carpoolclient.auth.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carpoolclient.R;

public class LandingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        findViewById(R.id.btn_get_started).setOnClickListener(v ->
                startActivity(new Intent(this, EmailVerification.class)));
    }
}


