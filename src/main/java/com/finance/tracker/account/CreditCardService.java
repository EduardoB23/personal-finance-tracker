package com.finance.tracker.account;

import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final ProfileRepository profileRepository;

    public CreditCardService(CreditCardRepository creditCardRepository, ProfileRepository profileRepository) {
        this.creditCardRepository = creditCardRepository;
        this.profileRepository = profileRepository;
    }

    public List<CreditCard> getAllCards() {
        return creditCardRepository.findAll();
    }

    @Transactional
    public CreditCard createCard(CreditCardRequest request) {
        Profile profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));

        CreditCard card = new CreditCard(
                request.name().trim(),
                request.limitAmount(),
                request.availableLimit(),
                request.closingDay(),
                request.dueDay(),
                profile
        );
        profile.getCards().add(card);
        return creditCardRepository.save(card);
    }
}
