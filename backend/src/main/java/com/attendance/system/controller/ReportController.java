package com.attendance.system.controller;

import com.attendance.system.dto.ReportRowDTO;
import com.attendance.system.model.domain.AttendanceRecord;
import com.attendance.system.model.domain.Employee;
import com.attendance.system.model.domain.LeaveRequest;
import com.attendance.system.model.domain.User;
import com.attendance.system.model.enums.LeaveRequestStatus;
import com.attendance.system.repository.AttendanceRecordRepository;
import com.attendance.system.repository.LeaveRequestRepository;
import com.attendance.system.service.EmployeeService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<ReportRowDTO>> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long employeeId,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<Employee> employees;
        if (!isAdmin) {
            employees = List.of(employeeService.findByUser(user));
        } else if (employeeId != null) {
            employees = List.of(employeeService.getEmployeeById(employeeId));
        } else {
            employees = employeeService.getAllEmployees();
        }

        List<ReportRowDTO> result = new ArrayList<>();

        for (Employee emp : employees) {
            if (department != null && !department.isBlank() && !department.equals("all")) {
                if (emp.getDepartment() == null || !emp.getDepartment().equalsIgnoreCase(department)) {
                    continue;
                }
            }

            List<AttendanceRecord> records = attendanceRecordRepository
                    .findByEmployeeIdAndCheckInTimeBetween(emp.getId(), fromDt, toDt);

            long daysPresent = records.stream()
                    .map(r -> r.getCheck_in_time().toLocalDate())
                    .distinct()
                    .count();

            double workedHours = records.stream()
                    .filter(r -> r.getWorked_hours() != null)
                    .mapToDouble(AttendanceRecord::getWorked_hours)
                    .sum();

            long lateCount = records.stream()
                    .filter(r -> "LATE".equalsIgnoreCase(r.getStatus()))
                    .count();

            List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeId(emp.getId())
                    .stream()
                    .filter(l -> l.getStatus() == LeaveRequestStatus.APPROVED)
                    .filter(l -> !l.getStartDate().isAfter(to) && !l.getEndDate().isBefore(from))
                    .collect(Collectors.toList());

            result.add(ReportRowDTO.builder()
                    .employeeId(emp.getId())
                    .employeeName(emp.getFirst_name() + " " + emp.getLast_name())
                    .department(emp.getDepartment())
                    .daysPresent(daysPresent)
                    .workedHours(Math.round(workedHours * 100.0) / 100.0)
                    .lateCount(lateCount)
                    .leaveRequestCount(leaves.size())
                    .build());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file,
                                        Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return ResponseEntity.status(403).body("Само администратор може да увезува податоци.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            List<String> lines = reader.lines().collect(Collectors.toList());
            int imported = 0;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isBlank()) continue;
                String[] parts = line.split(";");
                if (parts.length < 5) continue;
                try {
                    Long empId = Long.parseLong(parts[0].trim());
                    Employee emp = employeeService.getEmployeeById(empId);
                    AttendanceRecord record = new AttendanceRecord();
                    record.setEmployee(emp);
                    record.setCheck_in_time(LocalDateTime.parse(parts[1].trim()));
                    record.setCheck_out_time(LocalDateTime.parse(parts[2].trim()));
                    record.setStatus(parts[3].trim());
                    record.setWorked_hours(Double.parseDouble(parts[4].trim()));
                    attendanceRecordRepository.save(record);
                    imported++;
                } catch (Exception ignored) {}
            }
            return ResponseEntity.ok("Увезени " + imported + " записи.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Грешка при увоз: " + e.getMessage());
        }
    }
}