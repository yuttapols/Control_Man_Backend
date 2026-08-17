package com.controlm.shared.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlm.testsupport.PostgresIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@Tag("db")
@SpringBootTest
class HolidaySchemaMigrationTest extends PostgresIntegrationTest {
    private static final List<String> V2_TABLES = List.of(
            "calendar", "holiday", "holiday_revision", "holiday_revision_calendar");

    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway records V2 and the Phase 2 repeatable seed as successful")
    void v2AndReferenceSeedAreRecordedAsSuccessful() {
        Boolean v2 = jdbc.queryForObject(
                "select success from flyway_schema_history where version = '2'", Boolean.class);
        Integer seed = jdbc.queryForObject(
                "select count(*) from flyway_schema_history "
                        + "where version is null and script = 'R__phase2_reference_calendars.sql' and success",
                Integer.class);

        assertThat(v2).isTrue();
        assertThat(seed).isEqualTo(1);
    }

    @Test
    @DisplayName("V2 creates all holiday and calendar tables")
    void allV2TablesExist() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);
        assertThat(tables).containsAll(V2_TABLES);
    }

    @Test
    @DisplayName("revision history constraints and access-path indexes exist")
    void revisionConstraintsAndIndexesExist() {
        List<String> constraints = jdbc.queryForList(
                "select conname from pg_constraint where conrelid in "
                        + "('holiday'::regclass, 'holiday_revision'::regclass, "
                        + "'holiday_revision_calendar'::regclass)",
                String.class);
        List<String> indexes = jdbc.queryForList(
                "select indexname from pg_indexes where tablename in "
                        + "('holiday', 'holiday_revision', 'holiday_revision_calendar')",
                String.class);

        assertThat(constraints).contains(
                "fk_holiday_current_published_revision",
                "holiday_revision_calendar_pkey");
        assertThat(indexes).contains(
                "uq_holiday_one_current_revision",
                "ix_holiday_revision_date_status",
                "ix_holiday_revision_holiday_created",
                "ix_holiday_revision_published",
                "ix_holiday_revision_calendar_calendar");
    }

    @Test
    @DisplayName("government and bank reference calendars are active and unique")
    void referenceCalendarsAreSeeded() {
        List<String> codes = jdbc.queryForList(
                "select code from calendar where status = 'ACTIVE' order by code", String.class);
        assertThat(codes).contains("TH_BANK", "TH_GOVERNMENT");

        Integer duplicates = jdbc.queryForObject(
                "select count(*) from (select code from calendar group by code having count(*) > 1) x",
                Integer.class);
        assertThat(duplicates).isZero();
    }

    @Test
    @DisplayName("revision calendar mapping cascades only from its revision")
    void mappingForeignKeysHaveExpectedDeleteRules() {
        List<String> rules = jdbc.queryForList(
                "select delete_rule from information_schema.referential_constraints "
                        + "where constraint_schema = 'public' and constraint_name like "
                        + "'holiday_revision_calendar_%_fkey' order by constraint_name",
                String.class);
        assertThat(rules).containsExactlyInAnyOrder("CASCADE", "NO ACTION");
    }
}
