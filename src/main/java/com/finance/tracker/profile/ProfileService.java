package com.finance.tracker.profile;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Profile createProfile(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome do perfil é obrigatório");
        }

        Profile profile = new Profile(name.trim());
        return profileRepository.save(profile);
    }
}
