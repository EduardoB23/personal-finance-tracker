package com.finance.tracker.profile;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profiles")
    public List<Profile> getProfiles() {
        return profileService.getAllProfiles();
    }

    @PostMapping("/profiles")
    public ResponseEntity<Profile> createProfile(@Valid @RequestBody ProfileRequest request) {
        Profile created = profileService.createProfile(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
