package com.airpollution.survey.service;

import com.airpollution.survey.dto.UserCreateRequest;
import com.airpollution.survey.dto.PasswordChangeRequest;
import com.airpollution.survey.dto.UserResponse;
import com.airpollution.survey.dto.UserUpdateRequest;
import com.airpollution.survey.entity.AppUser;
import com.airpollution.survey.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String username = request.username().trim();
        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        });

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(normalizeRole(request.role()));
        user.setEnabled(true);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request, Authentication authentication) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(normalizeRole(request.role()));
        }
        if (request.enabled() != null) {
            if (user.getUsername().equals(authentication.getName()) && Boolean.FALSE.equals(request.enabled())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot disable your own account");
            }
            user.setEnabled(request.enabled());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getUsername().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }

        userRepository.delete(user);
    }

    @Transactional
    public void changeOwnPassword(PasswordChangeRequest request, Authentication authentication) {
        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ADMIN") && !normalized.equals("SURVEYOR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be ADMIN or SURVEYOR");
        }
        return normalized;
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), !Boolean.FALSE.equals(user.getEnabled()));
    }
}
