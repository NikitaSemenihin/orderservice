package com.innowise.orderservice.client;

import com.innowise.orderservice.model.dto.UserClientDto;

import java.util.Map;
import java.util.Set;

public interface UserServiceClient {
    UserClientDto getUserByEmail(String email);

    Map<String, UserClientDto> getUsersByEmails(Set<String> emails);
}
