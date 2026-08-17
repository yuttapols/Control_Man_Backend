package com.controlm.holiday.infrastructure.persistence;

import com.controlm.holiday.domain.HolidayType;
import com.controlm.holiday.domain.HolidayWorkflowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "holiday_revision", uniqueConstraints =
        @UniqueConstraint(columnNames = {"holiday_id", "revision_no"}))
public class HolidayRevisionEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "holiday_id", nullable = false)
    private HolidayEntity holiday;

    @Column(name = "revision_no", nullable = false)
    private int revisionNo;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "name_th", nullable = false, length = 250)
    private String nameTh;

    @Column(name = "name_en", length = 250)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false, length = 30)
    private HolidayType holidayType;

    @Column(name = "source_reference_no", length = 150)
    private String sourceReferenceNo;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "change_reason")
    private String changeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", nullable = false, length = 40)
    private HolidayWorkflowStatus workflowStatus;

    @Column(name = "published_at")
    private Instant publishedAt;

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

    protected HolidayRevisionEntity() {
    }

    HolidayRevisionEntity(UUID id, int revisionNo, LocalDate holidayDate, String nameTh,
            String nameEn, HolidayType holidayType, String sourceReferenceNo, String sourceUrl,
            String changeReason, HolidayWorkflowStatus workflowStatus, Instant publishedAt,
            UUID createdBy, UUID updatedBy, long version) {
        this.id = id;
        this.revisionNo = revisionNo;
        this.holidayDate = holidayDate;
        this.nameTh = nameTh;
        this.nameEn = nameEn;
        this.holidayType = holidayType;
        this.sourceReferenceNo = sourceReferenceNo;
        this.sourceUrl = sourceUrl;
        this.changeReason = changeReason;
        this.workflowStatus = workflowStatus;
        this.publishedAt = publishedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
    }

    void attachTo(HolidayEntity holiday) { this.holiday = holiday; }
    UUID getId() { return id; }
    HolidayEntity getHoliday() { return holiday; }
    int getRevisionNo() { return revisionNo; }
    LocalDate getHolidayDate() { return holidayDate; }
    String getNameTh() { return nameTh; }
    String getNameEn() { return nameEn; }
    HolidayType getHolidayType() { return holidayType; }
    String getSourceReferenceNo() { return sourceReferenceNo; }
    String getSourceUrl() { return sourceUrl; }
    String getChangeReason() { return changeReason; }
    HolidayWorkflowStatus getWorkflowStatus() { return workflowStatus; }
    Instant getPublishedAt() { return publishedAt; }
    UUID getCreatedBy() { return createdBy; }
    UUID getUpdatedBy() { return updatedBy; }
    long getVersion() { return version; }
}

