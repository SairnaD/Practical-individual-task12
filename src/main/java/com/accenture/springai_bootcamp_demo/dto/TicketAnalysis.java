package com.accenture.springai_bootcamp_demo.dto;

public record TicketAnalysis(Category category, Priority priority, String summary, boolean needsHuman) {
    public enum Category { BILLING, DELIVERY, TECHNICAL, ACCOUNT, OTHER }
    public enum Priority { LOW, MEDIUM, HIGH }
}