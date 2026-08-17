package com.controlm.holiday.application.port;

import com.controlm.holiday.domain.Holiday;
import java.util.Optional;
import java.util.UUID;

public interface HolidayRepository {
    Holiday save(Holiday holiday);
    Optional<Holiday> findById(UUID id);
    Optional<Holiday> findByHolidayCode(String holidayCode);
}
