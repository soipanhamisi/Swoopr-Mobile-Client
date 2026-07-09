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

        Log.d("FireBaseMessaging", "──── Firebase Message Received ────");
        Log.d("FireBaseMessaging", "Message ID   : " + message.getMessageId());
        Log.d("FireBaseMessaging", "From         : " + message.getFrom());

        // Log notification payload (shown when app is in foreground)
        if (message.getNotification() != null) {
            RemoteMessage.Notification notification = message.getNotification();
            Log.d("FireBaseMessaging", "Notif Title  : " + notification.getTitle());
            Log.d("FireBaseMessaging", "Notif Body   : " + notification.getBody());
        } else {
            Log.d("FireBaseMessaging", "Notification : (none)");
        }

        // Log data payload
        if (!message.getData().isEmpty()) {
            Log.d("FireBaseMessaging", "Data Payload : " + message.getData());
        } else {
            Log.d("FireBaseMessaging", "Data Payload : (empty)");
        }

        Log.d("FireBaseMessaging", "───────────────────────────────────");
    }
}
