package com.example.carpoolclient;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.carpoolclient.auth.storage.SecureTokenStore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FireBaseMessaging extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("FireBaseMessaging", "Refreshed token: " + token);
        SecureTokenStore.getInstance(this).saveFcmToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        Log.d("FireBaseMessaging", "Message received: " + message.getData());
    }
}
