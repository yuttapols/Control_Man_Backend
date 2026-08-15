package com.controlm.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation id to every request and echoes it back on the response.
 *
 * <p>Runs first so that anything failing later — including authentication — is still traceable
 * through the same id the client received.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = RequestIdHolder.acceptOrGenerate(request.getHeader(RequestIdHolder.HEADER));
        RequestIdHolder.set(requestId);
        response.setHeader(RequestIdHolder.HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            // Threads are pooled: leaving the id behind would mislabel the next request.
            RequestIdHolder.clear();
        }
    }
}
