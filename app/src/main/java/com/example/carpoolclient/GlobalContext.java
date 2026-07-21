package com.example.carpoolclient;

import android.app.Application;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalContext extends Application {
    private boolean isRegistered = false;
}

