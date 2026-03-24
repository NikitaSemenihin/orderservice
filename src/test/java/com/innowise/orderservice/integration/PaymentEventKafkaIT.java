package com.innowise.orderservice.integration;

import com.innowise.orderservice.event.PaymentCreatedEvent;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderStatus;
import com.innowise.orderservice.model.entity.PaymentStatus;
import com.innowise.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentEventKafkaIT {
    private static final String TOPIC = "payment.created";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.payment-created-topic", () -> TOPIC);
        registry.add("userservice.base-url", () -> "http://localhost:9999");
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");

    }

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void shouldUpdateOrderAfterPaymentEvent() throws Exception {
        Order order = new Order();
        order.setUserId(1L);
        order.setUserEmail("buyer@example.com");
        order.setStatus(OrderStatus.NEW);
        order.setTotalPrice(BigDecimal.valueOf(100));
        order.setDeleted(false);
        Order savedOrder = orderRepository.saveAndFlush(order);

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "payment-100",
                savedOrder.getId(),
                1L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-20T10:00:00Z"),
                BigDecimal.valueOf(100)
        );

        kafkaTemplate.send(TOPIC, event.id(), event).get();

        Order updatedOrder = waitForUpdatedOrder(savedOrder.getId());

        assertThat(updatedOrder.getPaymentId()).isEqualTo("payment-100");
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    private Order waitForUpdatedOrder(Long orderId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
        while (System.currentTimeMillis() < deadline) {
            Order order = orderRepository.findById(orderId).orElseThrow();
            if ("payment-100".equals(order.getPaymentId())) {
                return order;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Order was not updated after Kafka event");
    }
}
