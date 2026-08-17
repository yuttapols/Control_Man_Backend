package com.controlm.holiday.infrastructure.persistence;

import com.controlm.holiday.domain.Holiday;
import com.controlm.holiday.domain.HolidayRevision;
import com.controlm.holiday.domain.HolidayRevisionDetails;
import java.util.List;

final class HolidayPersistenceMapper {
    private HolidayPersistenceMapper() {
    }

    static HolidayEntity toEntity(Holiday holiday, boolean includePublishedPointer) {
        HolidayEntity entity = new HolidayEntity(holiday.id(), holiday.holidayCode(),
                holiday.substituteForId(), holiday.recordStatus(),
                includePublishedPointer ? holiday.currentPublishedRevisionId() : null,
                holiday.createdBy(), holiday.updatedBy(), holiday.version());
        holiday.revisions().stream().map(HolidayPersistenceMapper::toEntity).forEach(entity::addRevision);
        return entity;
    }

    static Holiday toDomain(HolidayEntity entity) {
        List<HolidayRevision> revisions = entity.getRevisions().stream()
                .map(HolidayPersistenceMapper::toDomain)
                .toList();
        return Holiday.rehydrate(entity.getId(), entity.getHolidayCode(), entity.getSubstituteForId(),
                entity.getRecordStatus(), entity.getCurrentPublishedRevisionId(), entity.getCreatedBy(),
                entity.getUpdatedBy(), entity.getVersion(), revisions);
    }

    private static HolidayRevisionEntity toEntity(HolidayRevision revision) {
        HolidayRevisionDetails details = revision.details();
        return new HolidayRevisionEntity(revision.id(), revision.revisionNo(), details.holidayDate(),
                details.nameTh(), details.nameEn(), details.holidayType(), details.sourceReferenceNo(),
                details.sourceUrl(), details.changeReason(), revision.workflowStatus(), revision.publishedAt(),
                revision.createdBy(), revision.updatedBy(), revision.version());
    }

    private static HolidayRevision toDomain(HolidayRevisionEntity entity) {
        HolidayRevisionDetails details = new HolidayRevisionDetails(entity.getHolidayDate(),
                entity.getNameTh(), entity.getNameEn(), entity.getHolidayType(),
                entity.getSourceReferenceNo(), entity.getSourceUrl(), entity.getChangeReason());
        return HolidayRevision.rehydrate(entity.getId(), entity.getHoliday().getId(), entity.getRevisionNo(),
                details, entity.getWorkflowStatus(), entity.getPublishedAt(), entity.getCreatedBy(),
                entity.getUpdatedBy(), entity.getVersion());
    }
}
