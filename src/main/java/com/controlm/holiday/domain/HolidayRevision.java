package com.controlm.holiday.domain;

import com.controlm.shared.error.BusinessRuleException;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class HolidayRevision {
    private final UUID id;
    private final UUID holidayId;
    private final int revisionNo;
    private HolidayRevisionDetails details;
    private final HolidayWorkflowStatus workflowStatus;
    private final Instant publishedAt;
    private final UUID createdBy;
    private UUID updatedBy;
    private final long version;

    private HolidayRevision(UUID id, UUID holidayId, int revisionNo, HolidayRevisionDetails details,
            HolidayWorkflowStatus workflowStatus, Instant publishedAt, UUID createdBy, UUID updatedBy,
            long version) {
        if (revisionNo < 1) {
            throw new BusinessRuleException("Revision number must be positive");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.holidayId = Objects.requireNonNull(holidayId, "holidayId");
        this.revisionNo = revisionNo;
        this.details = validate(details, revisionNo);
        this.workflowStatus = Objects.requireNonNull(workflowStatus, "workflowStatus");
        this.publishedAt = publishedAt;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.updatedBy = Objects.requireNonNull(updatedBy, "updatedBy");
        this.version = version;
    }

    static HolidayRevision draft(UUID holidayId, int revisionNo, HolidayRevisionDetails details, UUID actorId) {
        return new HolidayRevision(UUID.randomUUID(), holidayId, revisionNo, details,
                HolidayWorkflowStatus.DRAFT, null, actorId, actorId, 0);
    }

    public static HolidayRevision rehydrate(UUID id, UUID holidayId, int revisionNo,
            HolidayRevisionDetails details, HolidayWorkflowStatus workflowStatus, Instant publishedAt,
            UUID createdBy, UUID updatedBy, long version) {
        return new HolidayRevision(id, holidayId, revisionNo, details, workflowStatus, publishedAt,
                createdBy, updatedBy, version);
    }

    public void updateDraft(HolidayRevisionDetails replacement, UUID actorId) {
        if (workflowStatus != HolidayWorkflowStatus.DRAFT) {
            throw new BusinessRuleException("Only a Draft revision can be edited");
        }
        details = validate(replacement, revisionNo);
        updatedBy = Objects.requireNonNull(actorId, "actorId");
    }

    private static HolidayRevisionDetails validate(HolidayRevisionDetails value, int revisionNo) {
        Objects.requireNonNull(value, "details");
        if (value.holidayDate() == null || value.holidayType() == null || isBlank(value.nameTh())) {
            throw new BusinessRuleException("Holiday date, Thai name and type are required");
        }
        if (revisionNo > 1 && isBlank(value.changeReason())) {
            throw new BusinessRuleException("A change reason is required after the first revision");
        }
        if (!isBlank(value.sourceUrl())) {
            try {
                URI uri = URI.create(value.sourceUrl());
                if (!"https".equalsIgnoreCase(uri.getScheme()) || isBlank(uri.getHost())) {
                    throw new BusinessRuleException("Source URL must use HTTPS");
                }
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException("Source URL must use HTTPS");
            }
        }
        return new HolidayRevisionDetails(value.holidayDate(), value.nameTh().trim(), trim(value.nameEn()),
                value.holidayType(), trim(value.sourceReferenceNo()), trim(value.sourceUrl()),
                trim(value.changeReason()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public UUID id() { return id; }
    public UUID holidayId() { return holidayId; }
    public int revisionNo() { return revisionNo; }
    public HolidayRevisionDetails details() { return details; }
    public HolidayWorkflowStatus workflowStatus() { return workflowStatus; }
    public Instant publishedAt() { return publishedAt; }
    public UUID createdBy() { return createdBy; }
    public UUID updatedBy() { return updatedBy; }
    public long version() { return version; }
}
