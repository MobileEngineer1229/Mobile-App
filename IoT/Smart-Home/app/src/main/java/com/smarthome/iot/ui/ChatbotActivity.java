package com.smarthome.iot.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.smarthome.iot.R;
import com.smarthome.iot.models.ApiResponse;
import com.smarthome.iot.models.ChatbotResponse;
import com.smarthome.iot.network.ApiClient;
import com.smarthome.iot.network.ApiService;
import com.smarthome.iot.ui.adapters.ChatAdapter;
import com.smarthome.iot.utils.AuthManager;
import com.smarthome.iot.utils.MockDataProvider;
import com.smarthome.iot.utils.ThemeHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private TextInputEditText editTextMessage;
    private ImageButton buttonSend;
    private ProgressBar progressBar;
    private ChatAdapter chatAdapter;
    private ApiService apiService;
    private AuthManager authManager;
    private String conversationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize API
        ApiClient.initialize(this);
        apiService = ApiClient.getApiService();
        authManager = new AuthManager(this);

        String helpTopic = getIntent().getStringExtra("help_topic");
        String deviceName = getIntent().getStringExtra("device_name");

        initializeViews();
        setupInitialMessage(helpTopic, deviceName);
        loadChatHistory();
    }

    private void initializeViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        ImageButton buttonMore = findViewById(R.id.buttonMore);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        progressBar = findViewById(R.id.progressBar);

        chatAdapter = new ChatAdapter();
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(chatAdapter);

        buttonBack.setOnClickListener(v -> finish());
        buttonMore.setOnClickListener(v -> {
            // More options - could show clear history, settings, etc.
        });

        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText() != null ? 
                editTextMessage.getText().toString().trim() : "";
            if (!message.isEmpty()) {
                sendMessage(message);
                editTextMessage.setText("");
            }
        });
    }

    private void setupInitialMessage(String helpTopic, String deviceName) {
        String initialMessage = "Hello there! How can I assist you today?";
        
        if (helpTopic != null) {
            if (helpTopic.equals("device_setup")) {
                initialMessage = "I see you're setting up " + (deviceName != null ? deviceName : "a device") + 
                    ". I can help you with the setup process. What would you like to know?";
            } else if (helpTopic.equals("qr_scanning")) {
                initialMessage = "Having trouble scanning the QR code? I can help! Make sure you have good lighting and hold the camera steady.";
            }
        }
        
        chatAdapter.addMessage(new ChatAdapter.ChatMessage(initialMessage, true));
        scrollToBottom();
    }

    private void sendMessage(String message) {
        // Add user message immediately
        chatAdapter.addMessage(new ChatAdapter.ChatMessage(message, false));
        scrollToBottom();
        
        // Show loading
        showLoading(true);
        
        // Prepare request
        Map<String, String> messageData = new HashMap<>();
        messageData.put("message", message);
        if (conversationId != null) {
            messageData.put("conversationId", conversationId);
        }
        
        // Call API
        Call<ApiResponse<ChatbotResponse>> call = apiService.sendChatbotMessage(messageData);
        call.enqueue(new Callback<ApiResponse<ChatbotResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ChatbotResponse>> call, Response<ApiResponse<ChatbotResponse>> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ChatbotResponse chatbotResponse = response.body().getData();
                    if (chatbotResponse != null && chatbotResponse.getAssistantMessage() != null) {
                        // Add bot response
                        String botMessage = chatbotResponse.getAssistantMessage().getMessage();
                        chatAdapter.addMessage(new ChatAdapter.ChatMessage(botMessage, true));
                        scrollToBottom();
                        
                        // Store conversation ID if provided
                        if (chatbotResponse.getConversationId() != null) {
                            conversationId = chatbotResponse.getConversationId();
                        }
                    } else {
                        // Fallback to demo response
                        String botResponse = getBotResponse(message);
                        chatAdapter.addMessage(new ChatAdapter.ChatMessage(botResponse, true));
                        scrollToBottom();
                    }
                } else {
                    // Fallback to demo response on API error
                    String botResponse = getBotResponse(message);
                    chatAdapter.addMessage(new ChatAdapter.ChatMessage(botResponse, true));
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ChatbotResponse>> call, Throwable t) {
                showLoading(false);
                // Fallback to demo response on network error
                String botResponse = getBotResponse(message);
                chatAdapter.addMessage(new ChatAdapter.ChatMessage(botResponse, true));
                scrollToBottom();
            }
        });
    }
    
    private void loadChatHistory() {
        if (!authManager.isLoggedIn()) {
            return;
        }
        
        Call<ApiResponse<com.smarthome.iot.models.ChatHistoryResponse>> call = apiService.getChatHistory(1, 20);
        call.enqueue(new Callback<ApiResponse<com.smarthome.iot.models.ChatHistoryResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.smarthome.iot.models.ChatHistoryResponse>> call, 
                                 Response<ApiResponse<com.smarthome.iot.models.ChatHistoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.smarthome.iot.models.ChatHistoryResponse history = response.body().getData();
                    if (history != null && history.getMessages() != null && !history.getMessages().isEmpty()) {
                        // Load previous messages
                        for (com.smarthome.iot.models.ChatbotMessage msg : history.getMessages()) {
                            boolean isBot = "assistant".equals(msg.getRole());
                            chatAdapter.addMessage(new ChatAdapter.ChatMessage(msg.getMessage(), isBot));
                        }
                        scrollToBottom();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.smarthome.iot.models.ChatHistoryResponse>> call, Throwable t) {
                // Silently fail - just start fresh
            }
        });
    }
    
    private void showLoading(boolean show) {
        // ProgressBar might not exist in layout, so check first
        try {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            // ProgressBar doesn't exist, ignore
        }
        buttonSend.setEnabled(!show);
    }

    private void scrollToBottom() {
        recyclerViewMessages.post(() -> 
            recyclerViewMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
    }

    private String getBotResponse(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        // Greeting responses
        if (lowerMessage.contains("hi") || lowerMessage.contains("hello") || lowerMessage.contains("hey")) {
            return "Hello there! 👋 How can I assist you today?";
        }
        
        // Smartify account setup
        if (lowerMessage.contains("set up") || lowerMessage.contains("setup") || lowerMessage.contains("account")) {
            if (lowerMessage.contains("smartify")) {
                return "Awesome! 🎉 With Smartify, you can control devices, set up automation, manage energy, and more! What are you interested in exploring first?";
            }
            return "To set up your device, make sure Wi-Fi and Bluetooth are enabled. Then follow the on-screen instructions. Would you like step-by-step guidance?";
        }
        
        // Automation questions
        if (lowerMessage.contains("automation") || lowerMessage.contains("automate") || lowerMessage.contains("routine")) {
            return "Automation is a great feature! You can create routines that trigger multiple devices based on time, location, or other triggers. For example:\n\n" +
                   "• Morning routine: Turn on lights, adjust thermostat, play music\n" +
                   "• Away mode: Lock doors, turn off lights, enable security\n" +
                   "• Sleep routine: Dim lights, set temperature, turn off devices\n\n" +
                   "Would you like to learn how to set one up?";
        }
        
        // Device control
        if (lowerMessage.contains("device") || lowerMessage.contains("control") || lowerMessage.contains("turn on") || lowerMessage.contains("turn off")) {
            return "You can control your devices from the home screen! Tap on any device card to turn it on or off, adjust settings, or view details. Need help with a specific device?";
        }
        
        // QR code scanning
        if (lowerMessage.contains("qr") || lowerMessage.contains("scan") || lowerMessage.contains("code")) {
            return "For QR code scanning, ensure good lighting and hold your phone steady. If it doesn't work, you can enter the setup code manually. Need help with that?";
        }
        
        // Energy management
        if (lowerMessage.contains("energy") || lowerMessage.contains("consumption") || lowerMessage.contains("bill") || lowerMessage.contains("electricity")) {
            return "Smartify helps you track and manage your energy consumption! You can:\n\n" +
                   "• View real-time energy usage\n" +
                   "• Set energy-saving automations\n" +
                   "• Receive alerts for high consumption\n" +
                   "• Track monthly bills\n\n" +
                   "Check the Statistics tab to see your energy data!";
        }
        
        // Help and troubleshooting
        if (lowerMessage.contains("help") || lowerMessage.contains("trouble") || lowerMessage.contains("problem") || lowerMessage.contains("issue")) {
            return "I'm here to help! You can ask me about:\n\n" +
                   "• Device setup and connection\n" +
                   "• Creating automations\n" +
                   "• Energy management\n" +
                   "• Troubleshooting issues\n\n" +
                   "What specific issue are you facing?";
        }
        
        // Features and capabilities
        if (lowerMessage.contains("what") && (lowerMessage.contains("can") || lowerMessage.contains("do") || lowerMessage.contains("feature"))) {
            return "With Smartify, you can:\n\n" +
                   "🏠 Control all your smart devices from one app\n" +
                   "⚡ Monitor and manage energy consumption\n" +
                   "🤖 Create smart automations and routines\n" +
                   "📊 View detailed statistics and analytics\n" +
                   "🔔 Get notifications and alerts\n" +
                   "👥 Manage multiple homes and users\n\n" +
                   "What would you like to explore?";
        }
        
        // Default response
        return "I understand! Let me help you with that. Can you provide more details about what you're trying to do? I can assist with device setup, automation, energy management, and more!";
    }
}
