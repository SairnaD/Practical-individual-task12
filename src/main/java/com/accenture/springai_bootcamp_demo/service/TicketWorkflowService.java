package com.accenture.springai_bootcamp_demo.service;

import com.accenture.springai_bootcamp_demo.client.OpenRouterException;
import com.accenture.springai_bootcamp_demo.dto.Review;
import com.accenture.springai_bootcamp_demo.dto.TicketAnalysis;
import com.accenture.springai_bootcamp_demo.dto.TicketResult;
import com.accenture.springai_bootcamp_demo.tools.SupportTools;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TicketWorkflowService {

    private static final int MAX_REVISIONS = 1;
    private static final int MAX_ATTEMPTS = 3;

    private final ChatClient classifier;
    private final ChatClient responder;
    private final ChatClient reviewer;

    public TicketWorkflowService(ChatClient.Builder builder, SupportTools tools) {
        this.classifier = builder.clone()
                .defaultSystem("""
                        You are a support ticket classifier.
                        Return category, priority, a one-sentence summary,
                        and needsHuman=true only for legal threats, fraud accusations or abusive escalations.
                            Ordinary billing problems such as double charges are needsHuman=false.
                        """)
                .build();

        this.responder = builder.clone()
                .defaultSystem("""
                        You are a polite customer support agent.
                        Use the available tools to get order status and company policies.
                        Never invent order data or policies. If a tool returns nothing, say so.
                        Reply in the customer's language, in at most 5 sentences.
                        Never promise actions you cannot perform (forwarding feedback, callbacks, refunds). Only state facts from tools.
                        """)
                .defaultTools(tools)
                .build();

        this.reviewer = builder.clone()
                .defaultSystem("""
                You are a strict QA reviewer of support replies.
                You can use tools to verify order data and company policies.
                Approve if the reply is polite, answers the question, its facts match
                the tool data, and it makes no promises beyond standard policy.
                Do not ask for information that the tools cannot provide.
                If not approved, give short, concrete feedback.
                """)
                .defaultTools(tools)
                .build();
    }

    public TicketAnalysis analyze(String text) {
        return withRetry("classifier", () -> classifier.prompt()
                .user(u -> u.text("Ticket:\n{ticket}").param("ticket", text))
                .call()
                .entity(TicketAnalysis.class));
    }

    public TicketResult process(String text) {
        List<String> trace = new ArrayList<>();

        TicketAnalysis analysis = analyze(text);
        trace.add("classifier: " + analysis);

        if (analysis.needsHuman()) {
            trace.add("router: escalated to a human agent");
            return new TicketResult(analysis,
                    "Your request has been forwarded to a human specialist.",
                    new Review(true, "auto-escalated"), 0, trace);
        }

        String reply = draft(text, analysis, null);
        trace.add("responder: draft created");
        Review review = review(text, reply);
        trace.add("reviewer: " + review);

        int revisions = 0;
        while (!review.approved() && revisions < MAX_REVISIONS) {
            revisions++;
            reply = draft(text, analysis, review.feedback());
            trace.add("responder: revision " + revisions);
            review = review(text, reply);
            trace.add("reviewer: " + review);
        }

        if (!review.approved()) {
            trace.add("router: reply not approved after " + revisions + " revision(s), escalated to a human");
            return new TicketResult(analysis,
                    "Your request has been forwarded to a human specialist.",
                    review, revisions, trace);
        }
        return new TicketResult(analysis, reply, review, revisions, trace);
    }

    private String draft(String ticket, TicketAnalysis analysis, String feedback) {
        return withRetry("responder", () -> responder.prompt()
                .user(u -> u.text("""
                                Customer ticket:
                                {ticket}

                                Classification: category={category}, priority={priority}
                                Reviewer feedback to address (if any): {feedback}
                                """)
                        .param("ticket", ticket)
                        .param("category", analysis.category().name())
                        .param("priority", analysis.priority().name())
                        .param("feedback", feedback == null ? "none" : feedback))
                .call()
                .content());
    }

    private Review review(String ticket, String reply) {
        return withRetry("reviewer", () -> reviewer.prompt()
                .user(u -> u.text("Ticket:\n{ticket}\n\nDraft reply:\n{reply}")
                        .param("ticket", ticket)
                        .param("reply", reply))
                .call()
                .entity(Review.class));
    }

    private <T> T withRetry(String agent, Supplier<T> call) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                T result = call.get();
                if (result != null) {
                    return result;
                }
            } catch (RuntimeException ex) {
                last = ex;
                log.warn("Agent '{}' failed on attempt {}/{}: {}", agent, attempt, MAX_ATTEMPTS, ex.getMessage());
            }
        }
        throw new OpenRouterException("Agent '" + agent + "' failed after " + MAX_ATTEMPTS + " attempts", last);
    }
}