package com.accenture.springai_bootcamp_demo.dto;

import java.util.List;

public record TicketResult(TicketAnalysis analysis, String reply, Review review,
                           int revisions, List<String> trace) {
}
