package com.example.carpoolclient;

import android.app.Application;
import com.example.carpoolclient.utils.SecureTokenStore;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GlobalContext extends Application {
    private boolean isRegistered = false;
    private String fullName;
    private final Queue<String> tripEventsBufferQueue;
    private final Queue<String> chatMessagesBufferQueue;
    private volatile boolean chatScreenActive;

    public GlobalContext() {
        this.tripEventsBufferQueue = new ConcurrentLinkedQueue<>();
        this.chatMessagesBufferQueue = new ConcurrentLinkedQueue<>();
    }
    public void enqueue(String payload){
        tripEventsBufferQueue.add(payload);
    }
    public String dequeue(){
        return tripEventsBufferQueue.poll();
    }
    public void enqueueChatMessage(String payload) {
        chatMessagesBufferQueue.add(payload);
    }
    public String dequeueChatMessage() {
        return chatMessagesBufferQueue.poll();
    }
    public boolean isChatScreenActive() {
        return chatScreenActive;
    }
    public void setChatScreenActive(boolean chatScreenActive) {
        this.chatScreenActive = chatScreenActive;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Load persisted name from SecureTokenStore if available
        fullName = SecureTokenStore.getInstance(this).getFullName();
    }
}
