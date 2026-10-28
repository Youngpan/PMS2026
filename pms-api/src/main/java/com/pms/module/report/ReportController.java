package com.pms.module.report;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/night-audit")
    public Result<ReportService.NightAuditDraft> nightAudit(@RequestParam("date") LocalDate date) {
        return Result.ok(reportService.nightAuditDraft(date));
    }

    @PostMapping("/night-audit/confirm")
    public Result<NightAudit> confirm(@RequestBody Map<String, String> body) {
        return Result.ok(reportService.confirmNightAudit(LocalDate.parse(body.get("date"))));
    }

    @GetMapping("/revenue")
    public Result<ReportService.RevenueReport> revenue(@RequestParam("from") LocalDate from,
                                                        @RequestParam("to") LocalDate to) {
        return Result.ok(reportService.revenueReport(from, to));
    }

    @GetMapping("/folio-balance")
    public Result<?> folioBalance() {
        return Result.ok(reportService.folioBalance());
    }

    @GetMapping("/occupancy")
    public Result<ReportService.OccupancyReport> occupancy(@RequestParam("from") LocalDate from,
                                                           @RequestParam("to") LocalDate to) {
        return Result.ok(reportService.occupancy(from, to));
    }

    @GetMapping("/revenue/daily")
    public Result<java.util.List<ReportService.DailyRevenue>> revenueDaily(@RequestParam("from") LocalDate from,
                                                                            @RequestParam("to") LocalDate to) {
        return Result.ok(reportService.revenueDaily(from, to));
    }
}
