package br.com.conectasaude.agendamento.config;

import java.io.IOException;
import java.time.Clock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int requestsPerMinute;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Window> windows = new LinkedHashMap<>();

    public RateLimitFilter(@Value("${rate-limit.requests-per-minute:100}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/swagger-ui") || request.getRequestURI().startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }
        String client = request.getRemoteAddr();
        long now = clock.millis();
        synchronized (windows) {
            Window window = windows.get(client);
            if (window == null || now - window.startedAt() >= 60_000) {
                window = new Window(now, 0);
                windows.put(client, window);
            }
            if (window.count() >= requestsPerMinute) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                return;
            }
            windows.put(client, new Window(window.startedAt(), window.count() + 1));
            if (windows.size() > 10_000) {
                Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
                while (iterator.hasNext()) {
                    if (now - iterator.next().getValue().startedAt() >= 60_000 || windows.size() > 8_000) {
                        iterator.remove();
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private record Window(long startedAt, int count) {
    }
}
