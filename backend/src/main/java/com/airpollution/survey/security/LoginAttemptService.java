package com.airpollution.survey.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    public void assertNotLocked(String username) {
        Attempts attempts = attemptsByUsername.get(key(username));
        if (attempts != null && attempts.count >= MAX_ATTEMPTS && Instant.now().isBefore(attempts.lockedUntil)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in a few minutes.");
        }
    }

    public void recordFailure(String username) {
        attemptsByUsername.compute(key(username), (k, existing) -> {
            Attempts attempts = existing != null ? existing : new Attempts();
            attempts.count++;
            attempts.lockedUntil = Instant.now().plus(LOCKOUT_DURATION);
            return attempts;
        });
    }

    public void recordSuccess(String username) {
        attemptsByUsername.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Attempts {
        int count;
        Instant lockedUntil;
    }
}
