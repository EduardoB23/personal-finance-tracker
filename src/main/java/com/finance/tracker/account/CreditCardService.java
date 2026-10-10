package com.finance.tracker.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;

    public CreditCardService(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    public List<CreditCard> getAllCards() {
        return creditCardRepository.findAll();
    }

    @Transactional
    public CreditCard createCard(CreditCardRequest request) {
        return creditCardRepository.save(new CreditCard(
                request.name().trim(),
                request.limitAmount(),
                request.availableLimit(),
                request.closingDay(),
                request.dueDay()
        ));
    }
}
