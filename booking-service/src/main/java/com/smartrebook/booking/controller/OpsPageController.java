package com.smartrebook.booking.controller;

import com.smartrebook.booking.service.OpsQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@org.springframework.web.bind.annotation.RequestMapping("/ops")
public class OpsPageController {

    private final OpsQueryService opsQueryService;

    public OpsPageController(OpsQueryService opsQueryService) {
        this.opsQueryService = opsQueryService;
    }

    @GetMapping
    public String operations(Model model) {
        model.addAttribute("transactions", opsQueryService.listTransactions());
        model.addAttribute("reliability", opsQueryService.supplierReliability());
        return "operations";
    }

    @GetMapping("/transactions/{correlationId}")
    public String transactionDetail(@PathVariable String correlationId, Model model) {
        model.addAttribute("transaction", opsQueryService.getTransactionDetail(correlationId));
        return "transaction-details";
    }
}
