package com.controlm.iam.application;

import com.controlm.iam.application.port.AppUserRepository;
import com.controlm.iam.domain.AuthenticatedUser;
import com.controlm.iam.domain.LockoutPolicy;
import com.controlm.iam.domain.UserStatus;
import com.controlm.iam.infrastructure.persistence.AppUserEntity;
import com.controlm.shared.error.ApiException;
import com.controlm.shared.error.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies credentials against {@code app_user} and maintains the lockout counter.
 *
 * <p>Every failure path returns the same {@code UNAUTHENTICATED} error and message, so the
 * response never reveals whether a username exists, is inactive or is locked.
 */
@Service
public class AuthenticateCredentialsUseCaseImpl implements AuthenticateCredentialsUseCase {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthenticateCredentialsUseCaseImpl(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(String username, String rawPassword) {
        Optional<AppUserEntity> found = users.findByUsernameIgnoreCase(username);
        if (found.isEmpty()) {
            // Spend a hash comparison anyway so a missing user is not faster to reject than a
            // wrong password, which would leak which usernames exist by timing.
            passwordEncoder.matches(rawPassword, DUMMY_HASH);
            throw invalidCredentials();
        }

        AppUserEntity user = found.get();
        Instant now = Instant.now();

        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
        if (user.getStatus() != UserStatus.ACTIVE || locked) {
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            registerFailure(user, now);
            throw invalidCredentials();
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        users.save(user);
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getDisplayName());
    }

    private void registerFailure(AppUserEntity user, Instant now) {
        int failures = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(failures);
        if (LockoutPolicy.shouldLock(failures)) {
            user.setLockedUntil(now.plus(LockoutPolicy.LOCK_DURATION));
        }
        users.save(user);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(ErrorCode.UNAUTHENTICATED, "Invalid username or password");
    }

    /** A real bcrypt hash of a random value, used only to keep failure timing uniform. */
    private static final String DUMMY_HASH =
            "{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOa8x8p8h1oQ3d0m2fY7oXm9m1oGhP4bK";
}
