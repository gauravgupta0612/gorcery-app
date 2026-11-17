package com.golagrocery.config;

import com.golagrocery.user.Role;
import com.golagrocery.user.RoleRepository;
import com.golagrocery.user.User;
import com.golagrocery.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Creates the first ADMIN account from environment variables (ADMIN_PHONE / ADMIN_PASSWORD). */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdmin implements ApplicationRunner {
    private final AppProperties props;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var cfg = props.bootstrapAdmin();
        if (cfg == null || !StringUtils.hasText(cfg.phone()) || !StringUtils.hasText(cfg.password())) {
            log.info("ADMIN_PHONE/ADMIN_PASSWORD not set - skipping admin bootstrap");
            return;
        }
        if (cfg.password().length() < 10) {
            log.warn("ADMIN_PASSWORD must be at least 10 characters - admin account not created/updated");
            return;
        }
        var existing = users.findByPhone(cfg.phone());
        if (existing.isPresent()) {
            // ADMIN_PASSWORD is the source of truth: changing it in the environment resets the admin password on next start.
            User u = existing.get();
            boolean changed = false;
            if (u.getPasswordHash() == null || !encoder.matches(cfg.password(), u.getPasswordHash())) {
                u.setPasswordHash(encoder.encode(cfg.password()));
                changed = true;
            }
            if (u.getRoles().stream().noneMatch(r -> Role.ADMIN.equals(r.getName()))) {
                u.getRoles().add(roles.findByName(Role.ADMIN).orElseThrow());
                changed = true;
            }
            if (!u.isEnabled() || !u.isPhoneVerified()) {
                u.setEnabled(true);
                u.setPhoneVerified(true);
                changed = true;
            }
            if (changed) {
                users.save(u);
                log.info("Bootstrap admin account updated from ADMIN_PASSWORD");
            }
            return;
        }
        User u = new User();
        u.setFullName(cfg.name());
        u.setPhone(cfg.phone());
        u.setEmail(StringUtils.hasText(cfg.email()) ? cfg.email() : null);
        u.setPasswordHash(encoder.encode(cfg.password()));
        u.setPhoneVerified(true);
        u.getRoles().add(roles.findByName(Role.ADMIN).orElseThrow());
        users.save(u);
        log.info("Bootstrap admin account created for phone ending {}", cfg.phone().substring(Math.max(0, cfg.phone().length() - 4)));
    }
}
