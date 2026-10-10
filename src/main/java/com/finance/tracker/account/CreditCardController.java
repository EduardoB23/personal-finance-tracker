package com.finance.tracker.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CreditCardController {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    @GetMapping("/credit-cards")
    public List<CreditCard> getCreditCards() {
        return creditCardService.getAllCards();
    }

    @PostMapping("/credit-cards")
    public ResponseEntity<CreditCard> createCreditCard(@Valid @RequestBody CreditCardRequest request) {
        CreditCard created = creditCardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
