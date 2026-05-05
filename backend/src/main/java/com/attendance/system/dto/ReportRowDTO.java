package com.attendance.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRowDTO {
    private Long employeeId;
    private String employeeName;
    private String department;
    private long daysPresent;
    private double workedHours;
    private long lateCount;
    private long leaveRequestCount;
}