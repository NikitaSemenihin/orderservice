package com.innowise.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.model.entity.OrderStatus;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIT {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    private static final String SERVICE_NAME = "orderservice";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders_test")
            .withUsername("test")
            .withPassword("test");

    static final WireMockServer WIREMOCK = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        WIREMOCK.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("userservice.base-url", WIREMOCK::baseUrl);
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK.stop();
    }

    @BeforeEach
    void setUp() {
        WIREMOCK.resetAll();
        jdbcTemplate.execute("TRUNCATE TABLE order_items, orders, items RESTART IDENTITY CASCADE");
    }

    @Test
    void createOrderShouldPersistAndReturnUserPayloadFromWireMock() throws Exception {
        Item item = new Item();
        item.setName("Keyboard");
        item.setPrice(BigDecimal.valueOf(50));
        Item savedItem = itemRepository.saveAndFlush(item);

        stubUserByEmail("buyer@example.com");

        Map<String, Object> payload = Map.of(
                "userEmail", "buyer@example.com",
                "status", "NEW",
                "items", List.of(Map.of("itemId", savedItem.getId(), "quantity", 2))
        );

        mockMvc.perform(post("/api/orders")
                        .header(USER_ID_HEADER, "1")
                        .header(USER_ROLE_HEADER, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order.userEmail").value("buyer@example.com"))
                .andExpect(jsonPath("$.order.status").value("NEW"))
                .andExpect(jsonPath("$.order.totalPrice").value(100))
                .andExpect(jsonPath("$.user.email").value("buyer@example.com"));

        verify(getRequestedFor(urlPathEqualTo("/api/users"))
                .withQueryParam("email", equalTo("buyer@example.com"))
                .withHeader(SERVICE_NAME_HEADER, equalTo(SERVICE_NAME)));
    }

    @Test
    void getOrdersShouldUseBatchUsersEndpointAndReturnJoinedData() throws Exception {
        Item item = new Item();
        item.setName("Monitor");
        item.setPrice(BigDecimal.valueOf(70));
        Item savedItem = itemRepository.saveAndFlush(item);

        Order order = new Order();
        order.setUserEmail("page@example.com");
        order.setStatus(OrderStatus.PROCESSING);
        order.setDeleted(false);
        order.setTotalPrice(BigDecimal.valueOf(140));

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setItem(savedItem);
        orderItem.setQuantity(2);
        order.getOrderItems().add(orderItem);
        orderRepository.saveAndFlush(order);

        WIREMOCK.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo("/api/users/emails"))
                .willReturn(okJson(objectMapper.writeValueAsString(Map.of(
                        "page@example.com", buildUserBody("page@example.com")
                )))));

        mockMvc.perform(get("/api/orders")
                        .header(USER_ID_HEADER, "1")
                        .header(USER_ROLE_HEADER, "USER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].order.userEmail").value("page@example.com"))
                .andExpect(jsonPath("$.content[0].order.status").value("PROCESSING"))
                .andExpect(jsonPath("$.content[0].user.email").value("page@example.com"));

        verify(postRequestedFor(urlPathEqualTo("/api/users/emails"))
                .withHeader(SERVICE_NAME_HEADER, equalTo(SERVICE_NAME)));
    }

    @Test
    void getOrdersShouldReturnEmptyPageWithoutCallingUserserviceWhenNoOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .header(USER_ID_HEADER, "1")
                        .header(USER_ROLE_HEADER, "USER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(0, postRequestedFor(urlPathEqualTo("/api/users/emails")));
        verify(0, getRequestedFor(urlPathEqualTo("/api/users")));
    }

    private void stubUserByEmail(String email) throws Exception {
        WIREMOCK.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/api/users"))
                .withQueryParam("email", equalTo(email))
                .willReturn(okJson(objectMapper.writeValueAsString(buildUserBody(email)))));
    }

    private Map<String, Object> buildUserBody(String email) {
        return Map.of(
                "id", 1,
                "name", "John",
                "surname", "Doe",
                "email", email,
                "active", true,
                "birthDate", LocalDate.of(2000, 1, 1).toString(),
                "createdAt", "2024-01-01T00:00:00Z",
                "updatedAt", "2024-01-01T00:00:00Z"
        );
    }
}
