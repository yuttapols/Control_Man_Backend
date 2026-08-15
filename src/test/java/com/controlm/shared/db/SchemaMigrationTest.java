package com.controlm.shared.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that Flyway V1 actually applied against a real PostgreSQL instance, and that the
 * identity schema carries the constraints the design depends on. This is the first execution
 * of the V1 SQL, which was reviewed but never run before BE1-03.
 */
@Tag("db")
@SpringBootTest
class SchemaMigrationTest {

    private static final List<String> V1_TABLES = List.of(
            "user_level",
            "app_user",
            "role",
            "permission",
            "user_role",
            "role_permission",
            "auth_session",
            "system_setting",
            "audit_log");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway บันทึกว่า V1 ถูก apply สำเร็จ และไม่มี migration ที่ล้มเหลวค้างอยู่")
    void v1IsRecordedAsSuccessfullyApplied() {
        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);
        assertThat(success).as("V1 migration success flag").isTrue();

        Integer failed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        assertThat(failed).as("failed migrations").isZero();
    }

    @Test
    @DisplayName("ตารางทั้ง 9 ของ identity/RBAC schema ถูกสร้างครบตาม V1")
    void allV1TablesExist() {
        List<String> actual = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(actual).containsAll(V1_TABLES);
    }

    @Test
    @DisplayName("pgcrypto ถูกติดตั้ง เพื่อให้ gen_random_uuid() ใช้เป็น default ของ primary key ได้")
    void pgcryptoExtensionIsInstalled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'pgcrypto'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("username และ email ของ app_user ซ้ำกันแบบไม่สนตัวพิมพ์ไม่ได้ ตามกฎ normalize ในเอกสารออกแบบ")
    void appUserHasCaseInsensitiveUniqueIndexes() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'app_user'", String.class);
        assertThat(indexes).contains("uq_app_user_username_ci", "uq_app_user_email_ci");
    }

    @Test
    @DisplayName("ทุกตารางที่ต้องรองรับ optimistic locking มีคอลัมน์ version")
    void lockableTablesHaveVersionColumn() {
        for (String table : List.of("app_user", "role", "user_level")) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.columns "
                            + "WHERE table_name = ? AND column_name = 'version'",
                    Integer.class,
                    table);
            assertThat(count).as("version column on %s", table).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("คอลัมน์เวลาใช้ timestamptz ทั้งหมด ไม่ใช่ timestamp without time zone")
    void timestampColumnsAreTimezoneAware() {
        // flyway_schema_history is owned by Flyway, not our design; its installed_on column is a
        // plain timestamp by Flyway's own convention, so it is excluded from this check.
        List<String> wrongTypes = jdbcTemplate.queryForList(
                "SELECT table_name || '.' || column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND data_type = 'timestamp without time zone' "
                        + "AND table_name <> 'flyway_schema_history'",
                String.class);
        assertThat(wrongTypes).as("columns that should be timestamptz").isEmpty();
    }
}
