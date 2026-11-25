package com.example.outboxdebezium;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderEntity> create(
            @RequestParam("customer") String customer,
            @RequestParam("amount") BigDecimal amount) {
        OrderEntity createdOrder = orderService.createOrder(customer, amount);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping
    public List<OrderEntity> list() {
        return orderService.findAllOrders();
    }

    @GetMapping("/{id}")
    public OrderEntity getById(@PathVariable("id") Long id) {
        return orderService.findOrderById(id);
    }
}
