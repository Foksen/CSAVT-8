package ru.mirea.docsa2.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String url = request.getRequestURI();
        
        if (url.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String method = request.getMethod();
        String ip = getClientIP(request);
        
        MDC.put("http.method", method);
        MDC.put("http.url", url);
        MDC.put("http.client_ip", ip);
        
        log.info("Incoming request: {} {} from {}", method, url, ip);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("Response status: {} for {} {}", response.getStatus(), method, url);
            MDC.remove("http.method");
            MDC.remove("http.url");
            MDC.remove("http.client_ip");
        }
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}

