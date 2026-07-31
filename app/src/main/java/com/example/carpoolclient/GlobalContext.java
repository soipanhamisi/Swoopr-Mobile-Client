package com.example.carpoolclient;

import android.app.Application;
import com.example.carpoolclient.utils.SecureTokenStore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalContext extends Application {
    private boolean isRegistered = false;
    private String fullName;

    @Override
    public void onCreate() {
        super.onCreate();
        // Load persisted name from SecureTokenStore if available
        fullName = SecureTokenStore.getInstance(this).getFullName();
    }
}

