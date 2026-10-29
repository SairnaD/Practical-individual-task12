package com.accenture.springai_bootcamp_demo.tools;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Business capabilities exposed to the LLM. In a real app these would call
 * an order service / knowledge base; here they are mocked.
 */
@Slf4j
@Component
public class SupportTools {

    public record OrderInfo(String orderId, String status, String expectedDelivery) {
    }

    private static final Map<String, OrderInfo> ORDERS = Map.of(
            "1001", new OrderInfo("1001", "SHIPPED", "2026-11-02"),
            "1002", new OrderInfo("1002", "PROCESSING", "2026-11-07"),
            "1003", new OrderInfo("1003", "DELIVERED", "2026-10-25"));

    private static final Map<String, String> POLICIES = Map.of(
            "refund", "Refunds are possible within 30 days of delivery if the item is unused. Money is returned in 5-7 business days.",
            "delivery", "Standard delivery takes 3-7 business days. Delays over 3 days entitle the customer to a 10% discount code.",
            "warranty", "All devices have a 24-month warranty. Physical damage is not covered.");

    @Tool(description = "Get the current status and expected delivery date of an order by its id")
    public OrderInfo getOrderStatus(@ToolParam(description = "Order id, digits only, e.g. 1001") String orderId) {
        log.info("Tool getOrderStatus({})", orderId);
        return ORDERS.getOrDefault(orderId, new OrderInfo(orderId, "NOT_FOUND", "unknown"));
    }

    @Tool(description = "Search company policies (refund, delivery, warranty). Use a single keyword.")
    public String searchPolicies(@ToolParam(description = "One keyword, e.g. refund") String keyword) {
        log.info("Tool searchPolicies({})", keyword);
        String key = keyword.toLowerCase().trim();
        return POLICIES.entrySet().stream()
                .filter(e -> key.contains(e.getKey()) || e.getValue().toLowerCase().contains(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("No policy found for: " + keyword);
    }
}