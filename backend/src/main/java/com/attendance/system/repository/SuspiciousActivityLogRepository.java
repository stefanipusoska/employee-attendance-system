package com.attendance.system.repository;

import com.attendance.system.model.domain.SuspiciousActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SuspiciousActivityLogRepository extends JpaRepository<SuspiciousActivityLog, Long> {
}
