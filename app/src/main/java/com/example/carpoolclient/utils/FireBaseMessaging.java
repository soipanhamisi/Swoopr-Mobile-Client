package com.example.carpoolclient.utils;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.carpoolclient.GlobalContext;
import com.example.carpoolclient.dtos.ChatMessageDto;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class FireBaseMessaging extends FirebaseMessagingService {
    public static final String ACTION_CHAT_MESSAGE_RECEIVED = "com.example.carpoolclient.ACTION_CHAT_MESSAGE_RECEIVED";
    public static final String ACTION_TRIP_CREATION_EVENT = "com.swoopr.createTripEvents";
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
        String notificationType = message.getData().get("notificationType");
        String payload = message.getData().get("payload");
        if ("TRIP_CREATION".equals(notificationType)) {
            handleTripCreationEvens(payload);
            return;
        }
        dispatchChatPayload(payload);
    }

    private void handleTripCreationEvens(String payload) {
        GlobalContext context = (GlobalContext) getApplication();
        context.enqueue(payload);

        Intent intent = new Intent(ACTION_TRIP_CREATION_EVENT);
        intent.putExtra("status", payload);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
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

            GlobalContext context = (GlobalContext) getApplication();
            String normalizedPayload = gson.toJson(chatMessage);

            if (context.isChatScreenActive()) {
                Intent broadcastIntent = new Intent(ACTION_CHAT_MESSAGE_RECEIVED);
                broadcastIntent.setPackage(getPackageName());
                broadcastIntent.putExtra(EXTRA_CHAT_MESSAGE_JSON, normalizedPayload);
                sendBroadcast(broadcastIntent);
            } else {
                context.enqueueChatMessage(normalizedPayload);
            }
        } catch (JsonSyntaxException e) {
            Log.e("FireBaseMessaging", "Failed to parse chat payload", e);
        }
    }
}
