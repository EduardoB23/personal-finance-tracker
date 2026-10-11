package com.finance.tracker.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InvoiceController {

    private final InvoicePaymentService paymentService;

    public InvoiceController(InvoicePaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/credit-cards/{cardId}/invoices")
    public List<InvoicePaymentService.InvoiceView> getInvoices(@PathVariable Long cardId) {
        return paymentService.invoicesOf(cardId);
    }

    @PostMapping("/credit-cards/{cardId}/invoices/{billingMonth}/payments")
    public ResponseEntity<InvoicePaymentService.PaymentView> payInvoice(@PathVariable Long cardId,
                                                                       @PathVariable String billingMonth,
                                                                       @Valid @RequestBody InvoicePaymentService.PaymentRequest request) {
        InvoicePayment payment = paymentService.pay(cardId, parseMonth(billingMonth), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new InvoicePaymentService.PaymentView(payment.getId(),
                payment.getPaidOn(), payment.getAccount().getId(), payment.getProfile().getId(), payment.getAmount()));
    }

    @DeleteMapping("/invoice-payments/{paymentId}")
    public ResponseEntity<Void> refundPayment(@PathVariable Long paymentId) {
        paymentService.refund(paymentId);
        return ResponseEntity.noContent().build();
    }

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Mês inválido, use o formato AAAA-MM");
        }
    }
}
