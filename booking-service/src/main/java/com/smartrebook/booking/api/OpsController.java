package com.smartrebook.booking.api;

import com.smartrebook.booking.dto.ChangeRequestResponse;
import com.smartrebook.booking.dto.TransactionSummaryDto;
import com.smartrebook.booking.repository.jdbc.SupplierReliabilityRow;
import com.smartrebook.booking.service.OpsQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ops")
public class OpsController {

    private final OpsQueryService opsQueryService;

    public OpsController(OpsQueryService opsQueryService) {
        this.opsQueryService = opsQueryService;
    }

    @GetMapping("/transactions")
    public List<TransactionSummaryDto> transactions() {
        return opsQueryService.listTransactions();
    }

    @GetMapping("/transactions/{correlationId}")
    public ChangeRequestResponse transactionDetail(@PathVariable String correlationId) {
        return opsQueryService.getTransactionDetail(correlationId);
    }

    @GetMapping("/supplier-reliability")
    public List<SupplierReliabilityRow> supplierReliability() {
        return opsQueryService.supplierReliability();
    }
}
