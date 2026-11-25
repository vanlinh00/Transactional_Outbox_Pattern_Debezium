package com.example.outboxdebezium;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderEntity createOrder(String customerName, BigDecimal amount) {
        if (customerName == null || customerName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name must not be blank");
        }
        String sanitizedCustomer = customerName.trim();
        if (sanitizedCustomer.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name must be at most 255 characters");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order amount must be greater than zero");
        }

        OrderEntity order = orderRepository.save(new OrderEntity(sanitizedCustomer, amount, "CREATED"));

        String payload = serializeOrderCreatedPayload(order);
        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                "ORDER",
                String.valueOf(order.getId()),
                "OrderCreated",
                payload
        );
        outboxEventRepository.save(outboxEvent);

        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> findAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public OrderEntity findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private String serializeOrderCreatedPayload(OrderEntity order) {
        OrderCreatedPayload eventPayload = new OrderCreatedPayload(
                order.getId(),
                order.getCustomerName(),
                order.getAmount(),
                order.getStatus()
        );
        try {
            return objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OrderCreated outbox payload", ex);
        }
    }

    private record OrderCreatedPayload(Long orderId, String customer, BigDecimal amount, String status) {
    }
}
