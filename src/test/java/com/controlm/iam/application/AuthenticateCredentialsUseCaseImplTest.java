package com.controlm.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.controlm.iam.application.port.AppUserRepository;
import com.controlm.iam.domain.AuthenticatedUser;
import com.controlm.iam.domain.LockoutPolicy;
import com.controlm.iam.domain.UserStatus;
import com.controlm.iam.infrastructure.persistence.AppUserEntity;
import com.controlm.shared.error.ApiException;
import com.controlm.shared.error.ErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Pure unit test: a real delegating encoder and an in-memory user store, no Spring or database. */
class AuthenticateCredentialsUseCaseImplTest {

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private FakeAppUserRepository users;
    private AuthenticateCredentialsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        users = new FakeAppUserRepository();
        useCase = new AuthenticateCredentialsUseCaseImpl(users, encoder);
    }

    @Test
    @DisplayName("รหัสผ่านถูกต้องคืน identity และรีเซ็ตตัวนับ failed login กับบันทึกเวลาล็อกอิน")
    void correctPasswordReturnsIdentityAndResetsFailures() {
        AppUserEntity user = activeUser("Alice.Login", "correct-horse-battery");
        user.setFailedLoginCount(3);

        AuthenticatedUser result = useCase.authenticate("ALICE.LOGIN", "correct-horse-battery");

        assertThat(result.username()).isEqualTo("Alice.Login");
        assertThat(result.displayName()).isEqualTo("Display Name");
        assertThat(user.getFailedLoginCount()).as("failed count reset").isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).as("last login recorded").isNotNull();
    }

    @Test
    @DisplayName("รหัสผ่านผิดคืน UNAUTHENTICATED และเพิ่มตัวนับ failed login")
    void wrongPasswordThrowsGenericErrorAndCountsFailure() {
        AppUserEntity user = activeUser("Bob.Login", "correct-horse-battery");

        assertThatThrownBy(() -> useCase.authenticate("Bob.Login", "wrong-password"))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
        assertThat(user.getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("username ที่ไม่มีจริงคืน error ตัวเดียวกับรหัสผ่านผิด ไม่บอกใบ้ว่ามี user อยู่")
    void unknownUsernameFailsIdenticallyToWrongPassword() {
        assertThatThrownBy(() -> useCase.authenticate("ghost", "whatever"))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("บัญชีที่ไม่ ACTIVE (เช่น DISABLED) ล็อกอินไม่ได้แม้รหัสผ่านถูก")
    void inactiveAccountCannotAuthenticate() {
        AppUserEntity user = activeUser("Carol.Login", "correct-horse-battery");
        user.setStatus(UserStatus.DISABLED);

        assertThatThrownBy(() -> useCase.authenticate("Carol.Login", "correct-horse-battery"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("บัญชีที่ยัง locked_until อยู่ ถูกปฏิเสธโดยไม่ตรวจรหัสผ่านและตัวนับไม่ขยับ")
    void lockedAccountRejectedWithoutCheckingPassword() {
        AppUserEntity user = activeUser("Dave.Login", "correct-horse-battery");
        user.setLockedUntil(Instant.now().plusSeconds(600));
        user.setFailedLoginCount(LockoutPolicy.MAX_FAILED_ATTEMPTS);

        assertThatThrownBy(() -> useCase.authenticate("Dave.Login", "correct-horse-battery"))
                .isInstanceOf(ApiException.class);
        assertThat(user.getFailedLoginCount()).as("no extra increment while locked")
                .isEqualTo(LockoutPolicy.MAX_FAILED_ATTEMPTS);
    }

    @Test
    @DisplayName("การล้มครั้งที่ถึงเพดานตั้ง locked_until ให้บัญชี")
    void reachingThresholdLocksTheAccount() {
        AppUserEntity user = activeUser("Erin.Login", "correct-horse-battery");
        user.setFailedLoginCount(LockoutPolicy.MAX_FAILED_ATTEMPTS - 1);

        assertThatThrownBy(() -> useCase.authenticate("Erin.Login", "wrong-password"))
                .isInstanceOf(ApiException.class);
        assertThat(user.getFailedLoginCount()).isEqualTo(LockoutPolicy.MAX_FAILED_ATTEMPTS);
        assertThat(user.getLockedUntil()).as("account now locked").isNotNull();
    }

    private AppUserEntity activeUser(String username, String rawPassword) {
        AppUserEntity user = new AppUserEntity(
                username, username.toLowerCase() + "@example.com", "Display Name", encoder.encode(rawPassword));
        user.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        users.save(user);
        return user;
    }

    /** In-memory {@link AppUserRepository} so the use case can run without a database. */
    private static final class FakeAppUserRepository implements AppUserRepository {

        private final Map<UUID, AppUserEntity> byId = new HashMap<>();

        @Override
        public AppUserEntity save(AppUserEntity user) {
            byId.put(user.getId(), user);
            return user;
        }

        @Override
        public Optional<AppUserEntity> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<AppUserEntity> findByUsernameIgnoreCase(String username) {
            return byId.values().stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(username))
                    .findFirst();
        }
    }
}
