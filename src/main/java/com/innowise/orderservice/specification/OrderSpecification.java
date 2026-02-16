package com.innowise.orderservice.specification;

import com.innowise.orderservice.model.entity.Order;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public final class OrderSpecification {
    private OrderSpecification() {
    }

    public static Specification<Order> hasStatuses(List<String> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Order> createdAtBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("createdAt"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), to);
            }
            return cb.conjunction();
        };
    }
}
