package com.golagrocery.user;

import com.golagrocery.common.ApiException;
import com.golagrocery.security.CurrentUser;
import com.golagrocery.user.AuthDtos.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final RefreshTokenRepository refreshTokens;

    public record UpdateProfile(@NotBlank @Size(min = 2, max = 120) String fullName, @Email @Size(max = 160) String email,
                                @Pattern(regexp = "en|hi") String preferredLanguage) {}
    public record ChangePassword(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    @GetMapping
    @Transactional(readOnly = true)
    public UserDto me() {
        return UserDto.of(users.findById(CurrentUser.id()).orElseThrow(() -> ApiException.notFound("User")));
    }

    @PutMapping
    @Transactional
    public UserDto update(@Valid @RequestBody UpdateProfile req) {
        User u = users.findById(CurrentUser.id()).orElseThrow(() -> ApiException.notFound("User"));
        String email = StringUtils.hasText(req.email()) ? req.email().trim().toLowerCase() : null;
        if (email != null) {
            users.findByEmailIgnoreCase(email).filter(o -> !o.getId().equals(u.getId()))
                .ifPresent(o -> { throw ApiException.conflict("This email is already registered"); });
        }
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        if (req.preferredLanguage() != null) u.setPreferredLanguage(req.preferredLanguage());
        return UserDto.of(users.save(u));
    }

    @PostMapping("/password")
    @Transactional
    public void changePassword(@Valid @RequestBody ChangePassword req) {
        User u = users.findById(CurrentUser.id()).orElseThrow(() -> ApiException.notFound("User"));
        if (u.getPasswordHash() == null || !encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(u);
        refreshTokens.revokeAllForUser(u.getId());
    }
}
