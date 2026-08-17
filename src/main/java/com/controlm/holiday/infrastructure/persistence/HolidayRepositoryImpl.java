package com.controlm.holiday.infrastructure.persistence;

import com.controlm.holiday.application.port.HolidayRepository;
import com.controlm.holiday.domain.Holiday;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class HolidayRepositoryImpl implements HolidayRepository {
    private final HolidayJpaRepository jpa;

    HolidayRepositoryImpl(HolidayJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Holiday save(Holiday holiday) {
        UUID publishedRevisionId = holiday.currentPublishedRevisionId();
        HolidayEntity saved = jpa.saveAndFlush(HolidayPersistenceMapper.toEntity(holiday, false));
        if (publishedRevisionId != null) {
            saved.setCurrentPublishedRevisionId(publishedRevisionId);
            saved = jpa.saveAndFlush(saved);
        }
        return HolidayPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Holiday> findById(UUID id) {
        return jpa.findOneById(id).map(HolidayPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Holiday> findByHolidayCode(String holidayCode) {
        return jpa.findByHolidayCode(holidayCode).map(HolidayPersistenceMapper::toDomain);
    }
}
