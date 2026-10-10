package com.finance.tracker.account;

import com.finance.tracker.profile.Profile;
import com.finance.tracker.profile.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final ProfileRepository profileRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository, ProfileRepository profileRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.profileRepository = profileRepository;
    }

    public List<BankAccount> getAllAccounts() {
        return bankAccountRepository.findAll();
    }

    @Transactional
    public BankAccount createAccount(BankAccountRequest request) {
        Profile profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado"));

        BankAccount account = new BankAccount(request.name().trim(), request.type(), request.balance(), profile);
        account.setProfile(profile);
        profile.getAccounts().add(account);
        return bankAccountRepository.save(account);
    }
}
