package com.accenture.springai_bootcamp_demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank(message = "text must not be blank")
        @Size(max = 4000, message = "text must not exceed 4000 characters")
        String text) {
}