package com.smarthome.iot.models;

import java.util.List;

public class ChatHistoryResponse {
    private List<ChatbotMessage> messages;
    private int total;
    private int page;
    private int limit;
    private int totalPages;

    public List<ChatbotMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatbotMessage> messages) {
        this.messages = messages;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
