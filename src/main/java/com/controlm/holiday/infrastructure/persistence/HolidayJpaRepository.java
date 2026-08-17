package com.controlm.holiday.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface HolidayJpaRepository extends JpaRepository<HolidayEntity, UUID> {
    @EntityGraph(attributePaths = "revisions")
    Optional<HolidayEntity> findOneById(UUID id);

    @EntityGraph(attributePaths = "revisions")
    Optional<HolidayEntity> findByHolidayCode(String holidayCode);
}

