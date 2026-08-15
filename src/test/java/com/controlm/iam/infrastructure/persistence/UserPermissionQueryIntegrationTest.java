package com.controlm.iam.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@Tag("db")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserPermissionQueryIntegrationTest {
    @Autowired private AppUserJpaRepository users;
    @Autowired private RoleJpaRepository roles;
    @Autowired private PermissionJpaRepository permissions;
    @Autowired private UserRoleJpaRepository userRoles;
    @Autowired private RolePermissionJpaRepository rolePermissions;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("permission query คืนเฉพาะ grant จาก active role assignment ที่ยังไม่หมดอายุ")
    void permissionQueryHonoursRoleStatusAndValidityWindow() {
        AppUserEntity user = users.save(new AppUserEntity(
                "permission-" + System.nanoTime(),
                "permission-" + System.nanoTime() + "@example.com",
                "Permission Test",
                "{noop}unused"));
        RoleEntity role = roles.save(new RoleEntity("ROLE-" + System.nanoTime(), "ผู้แก้ไข"));
        PermissionEntity permission = permissions.save(new PermissionEntity(
                "holiday.revision.create." + System.nanoTime(), "holiday", "สร้างฉบับร่าง"));
        entityManager.flush();

        UserRoleEntity assignment = userRoles.save(new UserRoleEntity(user.getId(), role.getId()));
        rolePermissions.save(new RolePermissionEntity(role.getId(), permission.getId()));
        entityManager.flush();
        AppUserRepositoryImpl query = new AppUserRepositoryImpl(users);

        Instant activeAt = Instant.now();
        assertThat(query.findActivePermissionCodes(user.getId(), activeAt))
                .containsExactly(permission.getCode());

        assignment.setValidUntil(activeAt.plusSeconds(60));
        userRoles.save(assignment);
        entityManager.flush();
        assertThat(query.findActivePermissionCodes(user.getId(), activeAt.plusSeconds(61))).isEmpty();
    }
}
