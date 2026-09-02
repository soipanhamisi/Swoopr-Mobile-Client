package com.example.carpoolclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.example.carpoolclient.dtos.ChatMessageDto;
import com.example.carpoolclient.utils.FireBaseMessaging;
import com.example.carpoolclient.utils.SecureTokenStore;
import com.example.carpoolclient.utils.WebClient;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {
    private static final int MAX_PENDING_ECHOES = 50;
    private final Gson gson = new Gson();
    private final DateTimeFormatter backendTimestampFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DateTimeFormatter uiTimestampFormat = DateTimeFormatter.ofPattern("hh:mma\nM/d/yyyy", Locale.US);
    private final Set<String> pendingSelfEchoKeys = new LinkedHashSet<>();
    private final Deque<String> pendingSelfEchoOrder = new ArrayDeque<>();
    private final BroadcastReceiver chatMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIncomingPayload(intent.getStringExtra(FireBaseMessaging.EXTRA_CHAT_MESSAGE_JSON));
        }
    };

    private WebClient webClient;
    private EditText inputMessage;
    private ImageButton sendButton;
    private LinearLayout messagesContainer;
    private NestedScrollView chatScroll;
    private TextView membersNamesView;
    private boolean receiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_chat);

        webClient = new WebClient(this);
        inputMessage = findViewById(R.id.chat_message_input);
        sendButton = findViewById(R.id.chat_send_button);
        messagesContainer = findViewById(R.id.chat_messages_container);
        chatScroll = findViewById(R.id.chat_scroll);
        membersNamesView = findViewById(R.id.chat_members_names_value);
        ImageView navHome = findViewById(R.id.nav_home);

        sendButton.setOnClickListener(v -> sendChatMessage());
        navHome.setOnClickListener(v -> finish());
        bindMembersHeader();
        setupKeyboardInsetsHandling();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ((GlobalContext) getApplication()).setChatScreenActive(true);
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(FireBaseMessaging.ACTION_CHAT_MESSAGE_RECEIVED);
            ContextCompat.registerReceiver(this, chatMessageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
        drainBufferedChatMessages();
    }

    @Override
    protected void onStop() {
        ((GlobalContext) getApplication()).setChatScreenActive(false);
        if (receiverRegistered) {
            unregisterReceiver(chatMessageReceiver);
            receiverRegistered = false;
        }
        super.onStop();
    }

    private void sendChatMessage() {
        String messageText = inputMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, R.string.chat_message_required, Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);
        webClient.post("/messaging/postMessage", messageText, Void.class, (success, message, data) -> {
            sendButton.setEnabled(true);
            if (!success) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                return;
            }

            ChatMessageDto outboundMessage = new ChatMessageDto();
            outboundMessage.setSenderName(getDisplayName());
            outboundMessage.setMessage(messageText);
            outboundMessage.setTimeStamp(LocalDateTime.now().format(backendTimestampFormat));

            rememberOutboundForEchoSuppression(outboundMessage);
            addMessageCard(outboundMessage, true);
            inputMessage.setText("");
        });
    }

    private String getDisplayName() {
        String fullName = SecureTokenStore.getInstance(this).getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            return getString(R.string.chat_member_you_capitalized);
        }
        return fullName.trim();
    }

    private void addMessageCard(ChatMessageDto message, boolean outbound) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(outbound
                ? R.drawable.chat_message_outbound_background
                : R.drawable.chat_message_inbound_background);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        if (outbound) {
            cardParams.topMargin = dpToPx(12);
            cardParams.bottomMargin = dpToPx(20);
        } else {
            cardParams.topMargin = dpToPx(18);
        }
        card.setLayoutParams(cardParams);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView senderNameView = new TextView(this);
        senderNameView.setText(outbound ? getString(R.string.chat_member_you) : message.getSenderName());
        senderNameView.setTextColor(0xFF6A6A6A);
        senderNameView.setTextSize(20f);

        TextView timestampView = new TextView(this);
        LinearLayout.LayoutParams timestampParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        timestampView.setLayoutParams(timestampParams);
        timestampView.setTextAlignment(TextView.TEXT_ALIGNMENT_VIEW_END);
        timestampView.setText(formatTimestamp(message.getTimeStamp()));
        timestampView.setTextColor(0xFF888888);
        timestampView.setTextSize(14f);

        headerRow.addView(senderNameView);
        headerRow.addView(timestampView);

        TextView messageView = new TextView(this);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageParams.topMargin = dpToPx(4);
        messageView.setLayoutParams(messageParams);
        messageView.setText(message.getMessage());
        messageView.setTextColor(0xFF000000);
        messageView.setTextSize(15f);

        card.addView(headerRow);
        card.addView(messageView);
        messagesContainer.addView(card);
        chatScroll.post(() -> chatScroll.fullScroll(NestedScrollView.FOCUS_DOWN));
    }

    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.trim().isEmpty()) {
            return "";
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(rawTimestamp, backendTimestampFormat);
            return parsed.format(uiTimestampFormat);
        } catch (DateTimeParseException e) {
            return rawTimestamp;
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void drainBufferedChatMessages() {
        GlobalContext app = (GlobalContext) getApplication();
        String payload;
        while ((payload = app.dequeueChatMessage()) != null) {
            handleIncomingPayload(payload);
        }
    }

    private void handleIncomingPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return;
        }

        try {
            ChatMessageDto incomingMessage = gson.fromJson(payloadJson, ChatMessageDto.class);
            if (incomingMessage == null || incomingMessage.getMessage() == null || incomingMessage.getMessage().trim().isEmpty()) {
                return;
            }
            if (isExpectedSelfEcho(incomingMessage)) {
                return;
            }
            addMessageCard(incomingMessage, false);
        } catch (JsonSyntaxException e) {
            android.util.Log.e("ChatActivity", "Failed to parse incoming chat payload", e);
        }
    }

    private void rememberOutboundForEchoSuppression(ChatMessageDto outboundMessage) {
        String key = createEchoKey(outboundMessage.getSenderName(), outboundMessage.getMessage());
        if (key == null || pendingSelfEchoKeys.contains(key)) {
            return;
        }

        pendingSelfEchoKeys.add(key);
        pendingSelfEchoOrder.addLast(key);

        while (pendingSelfEchoOrder.size() > MAX_PENDING_ECHOES) {
            String oldest = pendingSelfEchoOrder.pollFirst();
            if (oldest != null) {
                pendingSelfEchoKeys.remove(oldest);
            }
        }
    }

    private boolean isExpectedSelfEcho(ChatMessageDto incomingMessage) {
        String key = createEchoKey(incomingMessage.getSenderName(), incomingMessage.getMessage());
        if (key == null || !pendingSelfEchoKeys.contains(key)) {
            return false;
        }

        pendingSelfEchoKeys.remove(key);
        pendingSelfEchoOrder.remove(key);
        return true;
    }

    private String createEchoKey(String senderName, String messageText) {
        if (senderName == null || messageText == null) {
            return null;
        }

        String normalizedSender = senderName.trim();
        String normalizedMessage = messageText.trim();
        if (normalizedSender.isEmpty() || normalizedMessage.isEmpty()) {
            return null;
        }

        return normalizedSender + "|" + normalizedMessage;
    }

    private void bindMembersHeader() {
        ArrayList<String> memberNames = getIntent().getStringArrayListExtra("TRIP_MEMBER_NAMES");
        if (memberNames == null) {
            String csvNames = getIntent().getStringExtra("TRIP_MEMBER_NAMES");
            if (csvNames != null && !csvNames.trim().isEmpty()) {
                memberNames = new ArrayList<>();
                for (String name : csvNames.split(",")) {
                    String trimmed = name.trim();
                    if (!trimmed.isEmpty()) {
                        memberNames.add(trimmed);
                    }
                }
            }
        }

        if (memberNames == null || memberNames.isEmpty()) {
            membersNamesView.setText(R.string.chat_member_you);
        } else {
            membersNamesView.setText(String.join(" ", memberNames));
        }
    }

    private void setupKeyboardInsetsHandling() {
        View root = findViewById(R.id.main);
        View chatHeader = findViewById(R.id.chat_header);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            // Header top margin for status bar
            ViewGroup.MarginLayoutParams headerParams = (ViewGroup.MarginLayoutParams) chatHeader.getLayoutParams();
            headerParams.topMargin = systemBars.top;
            chatHeader.setLayoutParams(headerParams);

            // Apply bottom padding to root to account for nav bar and keyboard
            v.setPadding(systemBars.left, 0, systemBars.right, Math.max(systemBars.bottom, ime.bottom));

            return WindowInsetsCompat.CONSUMED;
        });
    }
}
