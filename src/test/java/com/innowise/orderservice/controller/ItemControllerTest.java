package com.innowise.orderservice.controller;

import com.innowise.orderservice.exception.ExceptionHandler;
import com.innowise.orderservice.exception.ItemInUseException;
import com.innowise.orderservice.service.AccessPolicyService;
import com.innowise.orderservice.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@Import(ExceptionHandler.class)
class ItemControllerTest {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @MockBean
    private AccessPolicyService accessPolicyService;

    @Test
    void deleteItemShouldReturnConflictWhenItemInUse() throws Exception {
        doNothing().when(accessPolicyService).requireAdmin(org.mockito.ArgumentMatchers.any());
        doThrow(new ItemInUseException("Cannot delete an item that is in order"))
                .when(itemService).deleteItemById(5L);

        mockMvc.perform(delete("/api/items/{id}", 5L)
                        .header(USER_ID_HEADER, "1")
                        .header(USER_ROLE_HEADER, "ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot delete an item that is in order"))
                .andExpect(jsonPath("$.path").value("/api/items/5"));
    }
}

