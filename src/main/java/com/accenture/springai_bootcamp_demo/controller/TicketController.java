package com.accenture.springai_bootcamp_demo.controller;

import com.accenture.springai_bootcamp_demo.service.TicketWorkflowService;
import com.accenture.springai_bootcamp_demo.dto.TicketAnalysis;
import com.accenture.springai_bootcamp_demo.dto.TicketRequest;
import com.accenture.springai_bootcamp_demo.dto.TicketResult;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@AllArgsConstructor
public class TicketController {

    private final TicketWorkflowService workflow;

    @PostMapping("/analyze")
    public TicketAnalysis analyze(@Valid @RequestBody TicketRequest request) {
        return workflow.analyze(request.text());
    }

    @PostMapping("/process")
    public TicketResult process(@Valid @RequestBody TicketRequest request) {
        return workflow.process(request.text());
    }
}