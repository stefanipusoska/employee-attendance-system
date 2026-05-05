package com.attendance.system.repository;

import com.attendance.system.model.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByEmployeeIdAndCheckInTimeBetween(
        Long employeeId, LocalDateTime from, LocalDateTime to);
}