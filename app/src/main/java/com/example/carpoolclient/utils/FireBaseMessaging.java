package com.example.carpoolclient.utils;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.carpoolclient.dtos.ChatMessageDto;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class FireBaseMessaging extends FirebaseMessagingService {
    public static final String ACTION_CHAT_MESSAGE_RECEIVED = "com.example.carpoolclient.ACTION_CHAT_MESSAGE_RECEIVED";
    public static final String EXTRA_CHAT_MESSAGE_JSON = "extra_chat_message_json";
    private final Gson gson = new Gson();

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
            dispatchChatPayload(message.getData().get("payload"));
        } else {
            Log.d("FireBaseMessaging", "Data Payload : (empty)");
        }

        Log.d("FireBaseMessaging", "───────────────────────────────────");
    }

    private void dispatchChatPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return;
        }

        try {
            ChatMessageDto chatMessage = gson.fromJson(payload, ChatMessageDto.class);
            if (chatMessage == null || chatMessage.getMessage() == null || chatMessage.getMessage().trim().isEmpty()) {
                return;
            }

            Intent broadcastIntent = new Intent(ACTION_CHAT_MESSAGE_RECEIVED);
            broadcastIntent.setPackage(getPackageName());
            broadcastIntent.putExtra(EXTRA_CHAT_MESSAGE_JSON, gson.toJson(chatMessage));
            sendBroadcast(broadcastIntent);
        } catch (JsonSyntaxException e) {
            Log.e("FireBaseMessaging", "Failed to parse chat payload", e);
        }
    }
}
