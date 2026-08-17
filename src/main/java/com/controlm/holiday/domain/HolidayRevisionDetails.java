package com.controlm.holiday.domain;

import java.time.LocalDate;

public record HolidayRevisionDetails(
        LocalDate holidayDate,
        String nameTh,
        String nameEn,
        HolidayType holidayType,
        String sourceReferenceNo,
        String sourceUrl,
        String changeReason) {
}

