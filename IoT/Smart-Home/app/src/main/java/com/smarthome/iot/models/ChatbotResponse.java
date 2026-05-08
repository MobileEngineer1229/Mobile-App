package com.smarthome.iot.models;

public class ChatbotResponse {
    private ChatbotMessage userMessage;
    private ChatbotMessage assistantMessage;
    private String conversationId;

    public ChatbotMessage getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(ChatbotMessage userMessage) {
        this.userMessage = userMessage;
    }

    public ChatbotMessage getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(ChatbotMessage assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
