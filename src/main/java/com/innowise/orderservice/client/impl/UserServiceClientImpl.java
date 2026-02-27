package com.innowise.orderservice.client.impl;

import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.exception.RemoteUserNotFoundException;
import com.innowise.orderservice.exception.UserServiceUnavailableException;
import com.innowise.orderservice.model.dto.UserClientDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.Set;

@Service
public class UserServiceClientImpl implements UserServiceClient {

    private static final String USER_SERVICE_CB = "userServiceClient";
    private final RestClient restClient;

    public UserServiceClientImpl(@Qualifier("userServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @CircuitBreaker(name = USER_SERVICE_CB, fallbackMethod = "getUserByEmailFallback")
    public UserClientDto getUserByEmail(String email) {
        try {
            UserClientDto body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/users")
                            .queryParam("email", email)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new RemoteUserNotFoundException(
                                    String.format("User not found in user service by email: %s", email)
                            );
                        }
                        throw new UserServiceUnavailableException(
                                String.format("User service returned 4xx for email: %s", email)
                        );
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new UserServiceUnavailableException(
                                String.format("User service returned 5xx for email: %s", email)
                        );
                    })
                    .body(UserClientDto.class);

            if (body == null) {
                throw new UserServiceUnavailableException(
                        String.format("User service returned empty response for email: %s", email)
                );
            }
            return body;
        } catch (ResourceAccessException exception) {
            throw new UserServiceUnavailableException(
                    String.format("User service is unavailable (network/timeout) for email: %s", email),
                    exception
            );
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new RemoteUserNotFoundException(
                        String.format("User not found in user service by email: %s", email),
                        exception
                );
            }

            throw new UserServiceUnavailableException(
                    String.format("User service response error: %s", exception.getStatusCode()),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserServiceUnavailableException(
                    String.format("User service call failed for email: %s", email),
                    exception
            );
        }
    }

    private UserClientDto getUserByEmailFallback(String email, Throwable ex) {
        if (ex instanceof RemoteUserNotFoundException remoteUserNotFoundException) {
            throw remoteUserNotFoundException;
        }
        throw new UserServiceUnavailableException(
                String.format("User service call failed (circuit breaker) for email: %s", email),
                ex
        );
    }

    @Override
    @CircuitBreaker(name = USER_SERVICE_CB, fallbackMethod = "getUsersByEmailsFallback")
    public Map<String, UserClientDto> getUsersByEmails(Set<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, UserClientDto> body = restClient.post()
                    .uri("/api/users/emails")
                    .body(emails)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new RemoteUserNotFoundException(
                                    String.format("Users not found in user service for emails: %s", emails)
                            );
                        }
                        throw new UserServiceUnavailableException(
                                String.format("User service returned 4xx for email: %s", emails)
                        );
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new UserServiceUnavailableException(
                                String.format("User service returned 5xx for email: %s", emails)
                        );
                    })
                    .body(new ParameterizedTypeReference<Map<String, UserClientDto>>() {
                    });
            if (body == null) {
                throw new UserServiceUnavailableException(
                        String.format("User service returned empty response for emails: %s", emails)
                );
            }
            return body;
        } catch (ResourceAccessException exception) {
            throw new UserServiceUnavailableException(
                    String.format("User service is unavailable (network/timeout) for emails: %s", emails),
                    exception
            );
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new RemoteUserNotFoundException(
                        String.format("Users not found in user service for emails: %s", emails),
                        exception
                );
            }

            throw new UserServiceUnavailableException(
                    String.format("User service response error: %s", exception.getStatusCode()),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserServiceUnavailableException(
                    String.format("User service call failed for emails: %s", emails),
                    exception
            );
        }
    }

    public Map<String, UserClientDto> getUsersByEmailsFallback(Set<String> emails, Throwable ex) {
        if (ex instanceof RemoteUserNotFoundException remoteUserNotFoundException) {
            throw remoteUserNotFoundException;
        }
        throw new UserServiceUnavailableException(
                String.format("User service call failed (circuit breaker) for emails: %s", emails),
                ex
        );
    }
}
