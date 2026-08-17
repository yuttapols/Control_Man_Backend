package com.controlm.holiday.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlm.holiday.application.port.HolidayRepository;
import com.controlm.holiday.domain.Holiday;
import com.controlm.holiday.domain.HolidayRevisionDetails;
import com.controlm.holiday.domain.HolidayType;
import com.controlm.holiday.domain.HolidayRevision;
import com.controlm.holiday.domain.HolidayRecordStatus;
import com.controlm.holiday.domain.HolidayWorkflowStatus;
import java.time.Instant;
import com.controlm.iam.domain.UserStatus;
import com.controlm.iam.infrastructure.persistence.AppUserEntity;
import com.controlm.iam.infrastructure.persistence.AppUserJpaRepository;
import com.controlm.testsupport.PostgresIntegrationTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Tag("db")
@SpringBootTest
@Transactional
class HolidayPersistenceIntegrationTest extends PostgresIntegrationTest {
    @Autowired private HolidayRepository holidays;
    @Autowired private AppUserJpaRepository users;

    @Test
    void holidayAndFirstRevisionRoundTripWithoutLosingHistory() {
        UUID actorId = actor();
        Holiday original = Holiday.create("HOL-" + UUID.randomUUID(), details(null), actorId);

        Holiday saved = holidays.save(original);
        Holiday loaded = holidays.findById(saved.id()).orElseThrow();

        assertThat(loaded.holidayCode()).isEqualTo(original.holidayCode());
        assertThat(loaded.revisions()).singleElement().satisfies(revision -> {
            assertThat(revision.revisionNo()).isEqualTo(1);
            assertThat(revision.details().nameTh()).isEqualTo("วันทดสอบ");
            assertThat(revision.details().holidayDate()).isEqualTo(LocalDate.of(2027, 5, 1));
        });
    }

    @Test
    void editingDraftBumpsRevisionVersionAndKeepsRevisionIdentity() {
        UUID actorId = actor();
        Holiday saved = holidays.save(Holiday.create("HOL-" + UUID.randomUUID(), details(null), actorId));
        UUID revisionId = saved.revisions().getFirst().id();
        long oldVersion = saved.revisions().getFirst().version();

        saved.revisions().getFirst().updateDraft(new HolidayRevisionDetails(
                LocalDate.of(2027, 5, 2), "วันทดสอบแก้ไข", "Updated Test Holiday",
                HolidayType.SPECIAL, "REF-UPDATED", "https://example.test/updated", null), actorId);
        Holiday updated = holidays.save(saved);

        assertThat(updated.revisions()).singleElement().satisfies(revision -> {
            assertThat(revision.id()).isEqualTo(revisionId);
            assertThat(revision.version()).isGreaterThan(oldVersion);
            assertThat(revision.details().nameTh()).isEqualTo("วันทดสอบแก้ไข");
        });
    }

    @Test
    void addingDraftRevisionPreservesPublishedRevisionHistory() {
        UUID actorId = actor();
        UUID holidayId = UUID.randomUUID();
        HolidayRevision published = HolidayRevision.rehydrate(UUID.randomUUID(), holidayId, 1,
                details(null), HolidayWorkflowStatus.PUBLISHED, Instant.now(), actorId, actorId, 0);
        Holiday saved = holidays.save(Holiday.rehydrate(holidayId, "HOL-" + UUID.randomUUID(), null,
                HolidayRecordStatus.ACTIVE, published.id(), actorId, actorId, 0, java.util.List.of(published)));

        saved.addDraftRevision(details("Update for next publication"), actorId);
        Holiday updated = holidays.save(saved);

        assertThat(updated.currentPublishedRevisionId()).isEqualTo(published.id());
        assertThat(updated.revisions()).extracting(HolidayRevision::revisionNo).containsExactly(1, 2);
        assertThat(updated.revisions().getFirst().workflowStatus())
                .isEqualTo(HolidayWorkflowStatus.PUBLISHED);
        assertThat(updated.revisions().get(1).workflowStatus()).isEqualTo(HolidayWorkflowStatus.DRAFT);
    }

    private UUID actor() {
        String suffix = UUID.randomUUID().toString();
        AppUserEntity user = new AppUserEntity("holiday-" + suffix, "holiday-" + suffix + "@example.test",
                "Holiday Test Actor", "{noop}unused");
        user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user).getId();
    }

    private HolidayRevisionDetails details(String reason) {
        return new HolidayRevisionDetails(LocalDate.of(2027, 5, 1), "วันทดสอบ", "Test Holiday",
                HolidayType.REGULAR, "REF-TEST", "https://example.test/source", reason);
    }
}
