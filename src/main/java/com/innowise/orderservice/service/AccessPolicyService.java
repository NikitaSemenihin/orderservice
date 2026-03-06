package com.innowise.orderservice.service;

import com.innowise.orderservice.config.AuthContextInterceptor;
import com.innowise.orderservice.config.RequestAuthContext;
import com.innowise.orderservice.exception.ForbiddenException;
import com.innowise.orderservice.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class AccessPolicyService {

    public RequestAuthContext requireContext(HttpServletRequest request) {
        Object attribute = request.getAttribute(AuthContextInterceptor.AUTH_CONTEXT_ATTR);
        if (!(attribute instanceof RequestAuthContext context)) {
            throw new UnauthorizedException("Missing authentication context");
        }
        return context;
    }

    public void requireUserOrAdmin(HttpServletRequest request) {
        RequestAuthContext context = requireContext(request);
        if (!context.isEndUser()) {
            throw new ForbiddenException("Only USER or ADMIN can access this endpoint");
        }
    }

    public void requireAdmin(HttpServletRequest request) {
        RequestAuthContext context = requireContext(request);
        if (!context.isAdmin()) {
            throw new ForbiddenException("Admin role required");
        }
    }
}
