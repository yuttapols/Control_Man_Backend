package com.controlm.holiday.domain;

import com.controlm.shared.error.BusinessRuleException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Holiday {
    private final UUID id;
    private final String holidayCode;
    private final UUID substituteForId;
    private final HolidayRecordStatus recordStatus;
    private UUID currentPublishedRevisionId;
    private final UUID createdBy;
    private final UUID updatedBy;
    private final long version;
    private final List<HolidayRevision> revisions;

    private Holiday(UUID id, String holidayCode, UUID substituteForId, HolidayRecordStatus recordStatus,
            UUID currentPublishedRevisionId, UUID createdBy, UUID updatedBy, long version,
            List<HolidayRevision> revisions) {
        this.id = Objects.requireNonNull(id, "id");
        if (holidayCode == null || holidayCode.isBlank()) {
            throw new BusinessRuleException("Holiday code is required");
        }
        this.holidayCode = holidayCode.trim();
        this.substituteForId = substituteForId;
        this.recordStatus = Objects.requireNonNull(recordStatus, "recordStatus");
        this.currentPublishedRevisionId = currentPublishedRevisionId;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.updatedBy = Objects.requireNonNull(updatedBy, "updatedBy");
        this.version = version;
        this.revisions = new ArrayList<>(revisions);
        validateRevisionSequence();
        validatePublishedPointer();
    }

    public static Holiday create(String holidayCode, HolidayRevisionDetails firstRevision, UUID actorId) {
        UUID id = UUID.randomUUID();
        return new Holiday(id, holidayCode, null, HolidayRecordStatus.ACTIVE, null, actorId, actorId, 0,
                List.of(HolidayRevision.draft(id, 1, firstRevision, actorId)));
    }

    public static Holiday rehydrate(UUID id, String holidayCode, UUID substituteForId,
            HolidayRecordStatus recordStatus, UUID currentPublishedRevisionId, UUID createdBy,
            UUID updatedBy, long version, List<HolidayRevision> revisions) {
        return new Holiday(id, holidayCode, substituteForId, recordStatus, currentPublishedRevisionId,
                createdBy, updatedBy, version, revisions);
    }

    public HolidayRevision addDraftRevision(HolidayRevisionDetails details, UUID actorId) {
        if (revisions.stream().anyMatch(revision -> revision.workflowStatus() == HolidayWorkflowStatus.DRAFT)) {
            throw new BusinessRuleException("Finish the existing Draft before creating another revision");
        }
        int next = revisions.stream().map(HolidayRevision::revisionNo).max(Integer::compareTo).orElse(0) + 1;
        HolidayRevision revision = HolidayRevision.draft(id, next, details, actorId);
        revisions.add(revision);
        return revision;
    }

    public void pointToPublishedRevision(UUID revisionId) {
        HolidayRevision revision = revisions.stream()
                .filter(candidate -> candidate.id().equals(revisionId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Current published revision must belong to the same holiday"));
        if (revision.workflowStatus() != HolidayWorkflowStatus.PUBLISHED) {
            throw new BusinessRuleException("Current revision must be Published");
        }
        currentPublishedRevisionId = revisionId;
    }

    private void validateRevisionSequence() {
        List<Integer> numbers = revisions.stream().map(HolidayRevision::revisionNo).sorted().toList();
        for (int index = 0; index < numbers.size(); index++) {
            if (numbers.get(index) != index + 1) {
                throw new BusinessRuleException("Holiday revisions must be sequential");
            }
        }
        if (revisions.stream().anyMatch(revision -> !revision.holidayId().equals(id))) {
            throw new BusinessRuleException("Revision must belong to the same holiday");
        }
        revisions.sort(Comparator.comparingInt(HolidayRevision::revisionNo));
    }

    private void validatePublishedPointer() {
        if (currentPublishedRevisionId != null) {
            pointToPublishedRevision(currentPublishedRevisionId);
        }
    }

    public UUID id() { return id; }
    public String holidayCode() { return holidayCode; }
    public UUID substituteForId() { return substituteForId; }
    public HolidayRecordStatus recordStatus() { return recordStatus; }
    public UUID currentPublishedRevisionId() { return currentPublishedRevisionId; }
    public UUID createdBy() { return createdBy; }
    public UUID updatedBy() { return updatedBy; }
    public long version() { return version; }
    public List<HolidayRevision> revisions() { return List.copyOf(revisions); }
}

