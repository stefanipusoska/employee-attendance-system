package com.attendance.system.controller;

import com.attendance.system.model.domain.AttendanceRecord;
import com.attendance.system.model.domain.Employee;
import com.attendance.system.model.domain.SuspiciousActivityLog;
import com.attendance.system.service.SuspiciousActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suspicious-activity")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SuspiciousActivityLogController {
    private final SuspiciousActivityLogService suspiciousActivityLogService;

    @GetMapping
    public ResponseEntity<List<SuspiciousActivityLog>> getAllLogs(){
        return ResponseEntity.ok(suspiciousActivityLogService.getAllLogs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuspiciousActivityLog> getLogById(@PathVariable Long id){
        return ResponseEntity.ok(suspiciousActivityLogService.getLogById(id));
    }

    @PostMapping("/check-location")
    public ResponseEntity<?> checkLocation(@RequestBody AttendanceRecord record) {
        SuspiciousActivityLog log = suspiciousActivityLogService.checkLocation(record);
        if (log == null){
            return ResponseEntity.ok("Location is within allowed range, no suspicious activity detected");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @PostMapping("check-face-recognition")
    public ResponseEntity<?> checkFaceRecognition(
            @RequestBody Employee employee,
            @RequestParam boolean recognitionSuccessful,
            @RequestParam Double latitude,
            @RequestParam Double longitude){

        SuspiciousActivityLog log = suspiciousActivityLogService.checkFaceRecognition(employee, recognitionSuccessful, latitude, longitude);
        if (log == null){
            return ResponseEntity.ok("Face recognition succesful, no suspicious activity detected");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteLog(@PathVariable Long id){
        suspiciousActivityLogService.deleteLogById(id);
        return ResponseEntity.noContent().build();
    }
}
