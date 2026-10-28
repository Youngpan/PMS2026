package com.pms.module.folio;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/folios")
@RequiredArgsConstructor
public class FolioController {

    private final FolioService folioService;

    @GetMapping("/{id}")
    public Result<FolioService.FolioDetail> detail(@PathVariable Long id) {
        return Result.ok(folioService.detail(id));
    }

    @GetMapping("/by-stay/{stayId}")
    public Result<FolioService.FolioDetail> byStay(@PathVariable Long stayId) {
        com.pms.module.folio.Folio f = folioService.getByStayId(stayId);
        return Result.ok(folioService.detail(f.getId()));
    }

    @com.pms.common.OpLog(module = "M4", action = "录消费")
    @PostMapping("/{id}/items")
    public Result<FolioItem> addCharge(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String category = (String) body.getOrDefault("category", "杂费");
        String remark = (String) body.get("remark");
        return Result.ok(folioService.addCharge(id, category, amount, remark));
    }

    @com.pms.common.OpLog(module = "M4", action = "收押金")
    @PostMapping("/{id}/deposits")
    public Result<FolioItem> addDeposit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String method = (String) body.getOrDefault("method", "CASH");
        return Result.ok(folioService.addDeposit(id, amount, method));
    }

    @PostMapping("/{id}/transfer")
    public Result<Void> transfer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long toFolioId = ((Number) body.get("to_folio_id")).longValue();
        List<Long> items = ((List<Number>) body.get("items")).stream()
                .map(Number::longValue).collect(Collectors.toList());
        folioService.transfer(id, toFolioId, items);
        return Result.ok();
    }

    @PostMapping("/{id}/split")
    public Result<Void> split(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        List<FolioService.SplitItem> items = ((List<Map<String, Object>>) body.get("items")).stream()
                .map(m -> {
                    FolioService.SplitItem s = new FolioService.SplitItem();
                    s.itemId = ((Number) m.get("item_id")).longValue();
                    s.toFolioId = ((Number) m.get("to_folio_id")).longValue();
                    s.amount = new BigDecimal(m.get("amount").toString());
                    return s;
                }).collect(Collectors.toList());
        folioService.split(id, items);
        return Result.ok();
    }

    @com.pms.common.OpLog(module = "M4", action = "收款")
    @com.pms.common.Idempotent
    @PostMapping("/{id}/payments")
    public Result<Folio> pay(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String method = (String) body.get("method");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return Result.ok(folioService.pay(id, method, amount));
    }

    @PostMapping("/{id}/receipt")
    public Result<Map<String, Object>> receipt(@PathVariable Long id) {
        FolioService.FolioDetail d = folioService.detail(id);
        // 收据文本（实际可渲染 PDF）
        StringBuilder sb = new StringBuilder();
        sb.append("收据 Folio #").append(d.getFolio().getId()).append("\n");
        d.getItems().forEach(i -> sb.append(i.getCategory())
                .append("\t").append(i.getAmount()).append("\n"));
        sb.append("余额: ").append(d.getFolio().getBalance());
        return Result.ok(Map.of("text", sb.toString()));
    }
}
