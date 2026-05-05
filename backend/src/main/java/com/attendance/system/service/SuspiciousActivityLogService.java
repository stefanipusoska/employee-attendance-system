package com.attendance.system.service;

import com.attendance.system.model.domain.AttendanceRecord;
import com.attendance.system.model.domain.Employee;
import com.attendance.system.model.domain.SuspiciousActivityLog;
import com.attendance.system.repository.SuspiciousActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuspiciousActivityLogService {
    private final SuspiciousActivityLogRepository suspiciousActivityLogRepository;

    public List<SuspiciousActivityLog> getAllLogs(){
        return suspiciousActivityLogRepository.findAll();
    }

    public SuspiciousActivityLog getLogById(Long id){
        return suspiciousActivityLogRepository.findById(id).orElseThrow(()-> new RuntimeException("Suspicious activity log not found!"));
    }

    public SuspiciousActivityLog saveLog(SuspiciousActivityLog log){
        if (log.getCreated_at()==null){
            log.setCreated_at(LocalDateTime.now());
        }
        return suspiciousActivityLogRepository.save(log);
    }

    public void deleteLogById(Long id){
        SuspiciousActivityLog log = getLogById(id);
        suspiciousActivityLogRepository.delete(log);
    }

    public SuspiciousActivityLog checkLocation(AttendanceRecord record) {
        Employee employee = record.getEmployee();

        double distanceMeters = calculateDistance(
                record.getCheck_in_latitude(),
                record.getCheck_in_longitude(),
                employee.getAllowed_latitude(),
                employee.getAllowed_longitude()
        );

        if (distanceMeters > employee.getAllowed_radius_meters()) {
            SuspiciousActivityLog log = new SuspiciousActivityLog();
            log.setActivity_type("WRONG_LOCATION");
            log.setDescription(String.format(
                    "Employee checked in %.1f meters away from allowed location (max allowed: %.1f meters)",
                    distanceMeters, employee.getAllowed_radius_meters()
            ));
            log.setLatitude(record.getCheck_in_latitude());
            log.setLongitude(record.getCheck_in_longitude());
            log.setEmployee(employee);
            return saveLog(log);
        }

        return null; // no suspicious activity
    }

    // 2. Failed face recognition detection
    public SuspiciousActivityLog checkFaceRecognition(Employee employee, boolean recognitionSuccessful,
                                                      Double latitude, Double longitude) {
        if (!recognitionSuccessful) {
            SuspiciousActivityLog log = new SuspiciousActivityLog();
            log.setActivity_type("FAILED_FACE_RECOGNITION");
            log.setDescription("Employee face recognition failed during check-in attempt");
            log.setLatitude(latitude);
            log.setLongitude(longitude);
            log.setEmployee(employee);
            return saveLog(log);
        }

        return null; // no suspicious activity
    }

    // Haversine formula - calculates distance in meters between two GPS coordinates
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_METERS = 6_371_000;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                + Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
