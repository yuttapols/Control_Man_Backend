package com.controlm.iam.infrastructure.persistence;

import com.controlm.testsupport.PostgresIntegrationTest;
import static org.assertj.core.api.Assertions.assertThat;

import com.controlm.auth.application.port.AuthSessionRepository;
import com.controlm.auth.infrastructure.persistence.AuthSessionEntity;
import com.controlm.iam.application.port.AppUserRepository;
import com.controlm.iam.application.port.PermissionRepository;
import com.controlm.iam.application.port.RolePermissionRepository;
import com.controlm.iam.application.port.RoleRepository;
import com.controlm.iam.application.port.UserLevelRepository;
import com.controlm.iam.application.port.UserRoleRepository;
import com.controlm.iam.domain.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that the identity/RBAC JPA mappings agree with the Flyway V1 schema on a real
 * PostgreSQL instance: columns round-trip, {@code @Version} increments on update, and the
 * composite-key assignment tables load by their embedded id. Run with {@code ./mvnw verify -Pdb}.
 */
@Tag("db")
@SpringBootTest
@Transactional
class IdentityPersistenceMappingTest extends PostgresIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UserLevelRepository userLevelRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;


    @Test
    @DisplayName("user_level ถูก persist พร้อม id/created_at ที่ DB เติมให้ และ version เพิ่มเมื่อแก้ไข")
    void userLevelPersistsAndBumpsVersionOnUpdate() {
        UserLevelEntity level = userLevelRepository.save(new UserLevelEntity("L1-TEST", "ผู้ดูแล", 1));
        em.flush();

        assertThat(level.getId()).as("generated uuid").isNotNull();
        assertThat(level.getCreatedAt()).as("created_at default").isNotNull();
        assertThat(level.getVersion()).as("initial version").isZero();

        level.setNameTh("ผู้ดูแลระบบ");
        userLevelRepository.save(level);
        em.flush();

        assertThat(level.getVersion()).as("version after update").isEqualTo(1L);
    }

    @Test
    @DisplayName("app_user round-trip ครบทุกคอลัมน์ที่ map รวมถึง enum status และ FK user_level_id")
    void appUserRoundTripsMappedColumns() {
        UserLevelEntity level = userLevelRepository.save(new UserLevelEntity("L1-MAP", "ระดับ", 1));

        AppUserEntity user = new AppUserEntity("Alice.Test", "alice@example.com", "Alice", "{noop}hash");
        user.setUserLevelId(level.getId());
        user.setStatus(UserStatus.ACTIVE);
        appUserRepository.save(user);
        em.flush();
        em.clear();

        AppUserEntity loaded = appUserRepository.findById(user.getId()).orElseThrow();
        assertThat(loaded.getUsername()).isEqualTo("Alice.Test");
        assertThat(loaded.getEmail()).isEqualTo("alice@example.com");
        assertThat(loaded.getDisplayName()).isEqualTo("Alice");
        assertThat(loaded.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(loaded.getUserLevelId()).isEqualTo(level.getId());
        assertThat(loaded.getFailedLoginCount()).isZero();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("ค้นหา app_user ด้วย username แบบไม่สนตัวพิมพ์เจอ record เดียวกัน")
    void appUserLookupIsCaseInsensitive() {
        appUserRepository.save(new AppUserEntity("Bob.Case", "bob@example.com", "Bob", "{noop}hash"));
        em.flush();
        em.clear();

        assertThat(appUserRepository.findByUsernameIgnoreCase("bob.case")).isPresent();
        assertThat(appUserRepository.findByUsernameIgnoreCase("BOB.CASE")).isPresent();
    }

    @Test
    @DisplayName("ตาราง assignment แบบ composite key (user_role, role_permission) โหลดกลับด้วย embedded id ได้")
    void compositeKeyAssignmentsPersistAndLoad() {
        AppUserEntity user = appUserRepository.save(
                new AppUserEntity("Carol.Rbac", "carol@example.com", "Carol", "{noop}hash"));
        RoleEntity role = roleRepository.save(new RoleEntity("ROLE-MAP", "บทบาท"));
        PermissionEntity permission = permissionRepository.save(
                new PermissionEntity("holiday.revision.submit", "holiday", "ส่ง revision เพื่ออนุมัติ"));
        em.flush();

        userRoleRepository.save(new UserRoleEntity(user.getId(), role.getId()));
        rolePermissionRepository.save(new RolePermissionEntity(role.getId(), permission.getId()));
        em.flush();
        em.clear();

        assertThat(userRoleRepository.findById(new UserRoleId(user.getId(), role.getId())))
                .as("user_role by composite id")
                .isPresent();
        assertThat(rolePermissionRepository.findById(new RolePermissionId(role.getId(), permission.getId())))
                .as("role_permission by composite id")
                .isPresent();
    }

    @Test
    @DisplayName("auth_session เก็บเฉพาะ hash ของ refresh token และค้นด้วย hash เจอ")
    void authSessionStoresHashAndLooksUpByHash() {
        AppUserEntity user = appUserRepository.save(
                new AppUserEntity("Dave.Session", "dave@example.com", "Dave", "{noop}hash"));
        em.flush();

        String tokenHash = "sha256:" + UUID.randomUUID();
        authSessionRepository.save(new AuthSessionEntity(
                user.getId(), tokenHash, UUID.randomUUID(), Instant.now().plusSeconds(3600)));
        em.flush();
        em.clear();

        assertThat(authSessionRepository.findByRefreshTokenHash(tokenHash))
                .as("session found by refresh token hash")
                .isPresent();
    }

}
