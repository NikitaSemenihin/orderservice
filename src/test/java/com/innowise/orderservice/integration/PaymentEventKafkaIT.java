package com.innowise.orderservice.integration;

import com.innowise.orderservice.event.PaymentCreatedEvent;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderStatus;
import com.innowise.orderservice.model.entity.PaymentStatus;
import com.innowise.orderservice.repository.OrderRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentEventKafkaIT {
    private static final String TOPIC = "payment.created";
    private static final String DLT_TOPIC = TOPIC + ".DLT";

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

    @Test
    void shouldPublishEventToDltWhenOrderDoesNotExist() throws Exception {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "payment-missing-order",
                Long.MAX_VALUE,
                1L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-20T10:00:00Z"),
                BigDecimal.valueOf(100)
        );

        try (Consumer<String, PaymentCreatedEvent> consumer = createDltConsumer()) {
            consumer.subscribe(List.of(DLT_TOPIC));

            kafkaTemplate.send(TOPIC, event.id(), event).get();

            ConsumerRecord<String, PaymentCreatedEvent> dltRecord = waitForDltRecord(consumer);

            assertThat(dltRecord.topic()).isEqualTo(DLT_TOPIC);
            assertThat(dltRecord.key()).isEqualTo(event.id());
            assertThat(dltRecord.value()).isEqualTo(event);
            assertThat(orderRepository.findAll()).isEmpty();
        }
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

    private Consumer<String, PaymentCreatedEvent> createDltConsumer() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "payment-created-dlt-it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES, "com.innowise.orderservice.event",
                JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentCreatedEvent.class.getName(),
                JsonDeserializer.USE_TYPE_INFO_HEADERS, false
        );

        return new DefaultKafkaConsumerFactory<String, PaymentCreatedEvent>(props).createConsumer();
    }

    private ConsumerRecord<String, PaymentCreatedEvent> waitForDltRecord(Consumer<String, PaymentCreatedEvent> consumer)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, PaymentCreatedEvent> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, PaymentCreatedEvent> record : records) {
                if (DLT_TOPIC.equals(record.topic())) {
                    return record;
                }
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Event was not published to DLT");
    }
}
