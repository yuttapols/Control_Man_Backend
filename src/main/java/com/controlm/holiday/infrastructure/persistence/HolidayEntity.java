package com.controlm.holiday.infrastructure.persistence;

import com.controlm.holiday.domain.HolidayRecordStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "holiday")
public class HolidayEntity {
    @Id private UUID id;

    @Column(name = "holiday_code", nullable = false, unique = true, length = 80)
    private String holidayCode;

    @Column(name = "substitute_for_id")
    private UUID substituteForId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 30)
    private HolidayRecordStatus recordStatus;

    @Column(name = "current_published_revision_id")
    private UUID currentPublishedRevisionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Version private long version;

    @OneToMany(mappedBy = "holiday", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("revisionNo ASC")
    private List<HolidayRevisionEntity> revisions = new ArrayList<>();

    protected HolidayEntity() {
    }

    HolidayEntity(UUID id, String holidayCode, UUID substituteForId,
            HolidayRecordStatus recordStatus, UUID currentPublishedRevisionId,
            UUID createdBy, UUID updatedBy, long version) {
        this.id = id;
        this.holidayCode = holidayCode;
        this.substituteForId = substituteForId;
        this.recordStatus = recordStatus;
        this.currentPublishedRevisionId = currentPublishedRevisionId;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
    }

    void addRevision(HolidayRevisionEntity revision) {
        revisions.add(revision);
        revision.attachTo(this);
    }

    void setCurrentPublishedRevisionId(UUID currentPublishedRevisionId) {
        this.currentPublishedRevisionId = currentPublishedRevisionId;
    }

    UUID getId() { return id; }
    String getHolidayCode() { return holidayCode; }
    UUID getSubstituteForId() { return substituteForId; }
    HolidayRecordStatus getRecordStatus() { return recordStatus; }
    UUID getCurrentPublishedRevisionId() { return currentPublishedRevisionId; }
    UUID getCreatedBy() { return createdBy; }
    UUID getUpdatedBy() { return updatedBy; }
    long getVersion() { return version; }
    List<HolidayRevisionEntity> getRevisions() { return revisions; }
}
