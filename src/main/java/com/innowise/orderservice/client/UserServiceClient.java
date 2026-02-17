package com.innowise.orderservice.client;

import com.innowise.orderservice.model.dto.UserClientDto;

public interface UserServiceClient {
    UserClientDto getUserByEmail(String email);
}
