package com.controlm.holiday.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.controlm.shared.error.BusinessRuleException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HolidayTest {
    private final UUID actorId = UUID.randomUUID();

    @Test
    void createsFirstRevisionAsDraftNumberOne() {
        Holiday holiday = Holiday.create("NEW_YEAR", details(null), actorId);

        assertThat(holiday.revisions()).singleElement().satisfies(revision -> {
            assertThat(revision.revisionNo()).isEqualTo(1);
            assertThat(revision.workflowStatus()).isEqualTo(HolidayWorkflowStatus.DRAFT);
            assertThat(revision.holidayId()).isEqualTo(holiday.id());
        });
    }

    @Test
    void createsNextRevisionSequentiallyAfterPublishedRevision() {
        HolidayRevision published = revision(1, HolidayWorkflowStatus.PUBLISHED, null);
        Holiday holiday = rehydrate(List.of(published), published.id());

        HolidayRevision next = holiday.addDraftRevision(details("Changed official title"), actorId);

        assertThat(next.revisionNo()).isEqualTo(2);
        assertThat(holiday.revisions()).extracting(HolidayRevision::revisionNo).containsExactly(1, 2);
    }

    @Test
    void laterRevisionRequiresChangeReason() {
        HolidayRevision published = revision(1, HolidayWorkflowStatus.PUBLISHED, null);
        Holiday holiday = rehydrate(List.of(published), published.id());

        assertThatThrownBy(() -> holiday.addDraftRevision(details(null), actorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("change reason");
    }

    @Test
    void draftCanBeEdited() {
        Holiday holiday = Holiday.create("NEW_YEAR", details(null), actorId);
        HolidayRevision draft = holiday.revisions().getFirst();

        UUID editorId = UUID.randomUUID();
        draft.updateDraft(new HolidayRevisionDetails(LocalDate.of(2027, 1, 2), "วันปีใหม่ใหม่",
                "Updated New Year", HolidayType.SPECIAL, "REF-2", "https://example.test/2", null),
                editorId);

        assertThat(draft.details().holidayDate()).isEqualTo(LocalDate.of(2027, 1, 2));
        assertThat(draft.details().nameTh()).isEqualTo("วันปีใหม่ใหม่");
        assertThat(draft.updatedBy()).isEqualTo(editorId);
    }

    @Test
    void submittedAndPublishedRevisionsAreImmutable() {
        for (HolidayWorkflowStatus status : List.of(
                HolidayWorkflowStatus.PENDING_LEVEL_1, HolidayWorkflowStatus.PUBLISHED)) {
            HolidayRevision revision = revision(1, status, null);
            assertThatThrownBy(() -> revision.updateDraft(details(null), actorId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Draft");
        }
    }

    @Test
    void rejectsNonHttpsSourceUrl() {
        HolidayRevisionDetails invalid = new HolidayRevisionDetails(LocalDate.of(2027, 1, 1),
                "วันปีใหม่", "New Year", HolidayType.REGULAR, "REF-1",
                "http://example.test/source", null);

        assertThatThrownBy(() -> Holiday.create("NEW_YEAR", invalid, actorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void publishedPointerMustBelongToHolidayAndBePublished() {
        Holiday holiday = Holiday.create("NEW_YEAR", details(null), actorId);

        assertThatThrownBy(() -> holiday.pointToPublishedRevision(UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("same holiday");
        assertThatThrownBy(() -> holiday.pointToPublishedRevision(holiday.revisions().getFirst().id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Published");
    }

    private Holiday rehydrate(List<HolidayRevision> revisions, UUID publishedId) {
        return Holiday.rehydrate(revisions.getFirst().holidayId(), "NEW_YEAR", null,
                HolidayRecordStatus.ACTIVE, publishedId, actorId, actorId, 0, revisions);
    }

    private HolidayRevision revision(int number, HolidayWorkflowStatus status, String changeReason) {
        UUID holidayId = UUID.randomUUID();
        return HolidayRevision.rehydrate(UUID.randomUUID(), holidayId, number, details(changeReason), status,
                status == HolidayWorkflowStatus.PUBLISHED ? Instant.now() : null, actorId, actorId, 0);
    }

    private HolidayRevisionDetails details(String changeReason) {
        return new HolidayRevisionDetails(LocalDate.of(2027, 1, 1), "วันปีใหม่", "New Year",
                HolidayType.REGULAR, "REF-1", "https://example.test/source", changeReason);
    }
}
