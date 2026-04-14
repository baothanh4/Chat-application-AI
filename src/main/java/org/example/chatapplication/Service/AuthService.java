package org.example.chatapplication.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapplication.DTO.Request.AuthLoginRequest;
import org.example.chatapplication.DTO.Request.AuthRegisterRequest;
import org.example.chatapplication.DTO.Response.FaceLoginCandidateResponse;
import org.example.chatapplication.DTO.Response.UserResponse;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.example.chatapplication.Repository.UserAccountRepository;
import org.example.chatapplication.Controller.FaceLoginAmbiguousException;
import org.example.chatapplication.Controller.FaceEnrollmentConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private static final int FACE_MATCH_THRESHOLD = 140;
    private static final int FACE_AMBIGUITY_MARGIN = 30;

    private final UserAccountRepository userAccountRepository;
    private final UserAccountService userAccountService;
    private final PasswordHasher passwordHasher;
    private final FileStorageService fileStorageService;
    private final FaceVerificationService faceVerificationService;
    private final PresenceService presenceService;

    @Transactional
    public UserResponse register(AuthRegisterRequest request) {
        UserAccount user = createUser(request);
        UserAccount saved = userAccountRepository.save(user);
        presenceService.markOnline(saved.getId());
        return userAccountService.toResponse(saved);
    }

    @Transactional
    public UserResponse registerWithFace(AuthRegisterRequest request, MultipartFile faceImage) {
        UserAccount user = createUser(request);
        String faceSignature = faceVerificationService.generateSignature(faceImage);
        ensureFaceIsUnique(faceSignature);
        String faceTemplatePath = fileStorageService.storeFaceTemplate(faceImage);
        user.setFaceTemplatePath(faceTemplatePath);
        user.setFaceTemplateHash(faceSignature);
        user.setFaceLoginEnabled(true);
        user.setFaceEnrolledAt(Instant.now());

        UserAccount saved = userAccountRepository.save(user);
        presenceService.markOnline(saved.getId());
        return userAccountService.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse login(AuthLoginRequest request) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (user.getPasswordHash() == null || !passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        presenceService.markOnline(user.getId());
        return userAccountService.toResponse(userAccountRepository.findById(user.getId()).orElse(user));
    }

    @Transactional
    public UserResponse loginWithFace(MultipartFile faceImage) {
        String scannedSignature = faceVerificationService.generateSignature(faceImage);
        List<UserAccount> candidates = userAccountRepository.findByFaceLoginEnabledTrueAndFaceTemplateHashIsNotNull();
        List<FaceMatch> rankedMatches = candidates.stream()
                .map(user -> new FaceMatch(user, faceDistance(user, scannedSignature)))
                .sorted(Comparator.comparingInt(FaceMatch::distance))
                .toList();

        if (log.isDebugEnabled()) {
            log.debug("Face login scan started. candidates={}, threshold={}, ambiguityMargin={}", rankedMatches.size(), FACE_MATCH_THRESHOLD, FACE_AMBIGUITY_MARGIN);
            rankedMatches.stream().limit(5).forEach(match -> log.debug(
                    "Face login candidate: userId={}, username={}, displayName={}, distance={}, templatePath={}",
                    match.user().getId(),
                    match.user().getUsername(),
                    match.user().getDisplayName(),
                    match.distance(),
                    match.user().getFaceTemplatePath()
            ));
        }

        FaceMatch bestMatch = rankedMatches.stream()
                .filter(match -> match.distance() <= FACE_MATCH_THRESHOLD)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Face scan does not match any registered user"));

        int secondBestDistance = rankedMatches.stream()
                .filter(match -> match.distance() <= FACE_MATCH_THRESHOLD)
                .skip(1)
                .map(FaceMatch::distance)
                .findFirst()
                .orElse(Integer.MAX_VALUE);

        if (secondBestDistance != Integer.MAX_VALUE && secondBestDistance - bestMatch.distance() <= FACE_AMBIGUITY_MARGIN) {
            List<FaceLoginCandidateResponse> topCandidates = rankedMatches.stream()
                    .filter(match -> match.distance() <= FACE_MATCH_THRESHOLD)
                    .limit(3)
                    .map(this::toCandidateResponse)
                    .toList();
            if (log.isDebugEnabled()) {
                log.debug("Face scan ambiguous. bestUserId={}, bestDistance={}, secondBestDistance={}, delta={}",
                        bestMatch.user().getId(), bestMatch.distance(), secondBestDistance, secondBestDistance - bestMatch.distance());
            }
            throw new FaceLoginAmbiguousException("Face scan is ambiguous. Please choose the correct account.", topCandidates, FACE_MATCH_THRESHOLD, FACE_AMBIGUITY_MARGIN);
        }

        if (log.isDebugEnabled()) {
            log.debug("Face scan matched. userId={}, username={}, distance={}", bestMatch.user().getId(), bestMatch.user().getUsername(), bestMatch.distance());
        }

        presenceService.markOnline(bestMatch.user().getId());
        return userAccountService.toResponse(userAccountRepository.findById(bestMatch.user().getId()).orElse(bestMatch.user()));
    }

    @Transactional
    public UserResponse confirmFaceLogin(MultipartFile faceImage, UUID selectedUserId) {
        if (selectedUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is required");
        }

        String scannedSignature = faceVerificationService.generateSignature(faceImage);
        UserAccount selectedUser = userAccountService.requireUser(selectedUserId);
        int distance = faceDistance(selectedUser, scannedSignature);

        if (log.isDebugEnabled()) {
            log.debug("Face login confirm. selectedUserId={}, username={}, distance={}, threshold={}",
                    selectedUser.getId(), selectedUser.getUsername(), distance, FACE_MATCH_THRESHOLD);
        }

        if (distance > FACE_MATCH_THRESHOLD) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Selected account does not match the scanned face");
        }

        presenceService.markOnline(selectedUser.getId());
        return userAccountService.toResponse(userAccountRepository.findById(selectedUser.getId()).orElse(selectedUser));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserAccount createUser(AuthRegisterRequest request) {
        String username = request.getUsername().trim();
        if (userAccountRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(request.getDisplayName().trim());
        user.setFullName(trimToNull(request.getFullName()));
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        user.setAddress(trimToNull(request.getAddress()));
        user.setBio(trimToNull(request.getBio()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(trimToNull(request.getGender()));
        user.setEmail(request.getEmail());
        user.setAvatarPath(trimToNull(request.getAvatarPath()));
        user.setPasswordHash(passwordHasher.hash(request.getPassword()));
        user.setFaceLoginEnabled(false);
        return user;
    }

    private void ensureFaceIsUnique(String candidateSignature) {
        List<UserResponse> conflicts = userAccountRepository.findByFaceLoginEnabledTrueAndFaceTemplateHashIsNotNull().stream()
                .filter(existing -> faceDistance(existing, candidateSignature) <= FACE_MATCH_THRESHOLD)
                .map(userAccountService::toResponse)
                .toList();

        if (!conflicts.isEmpty()) {
            throw new FaceEnrollmentConflictException("This face is already enrolled for another account", conflicts);
        }
    }

    private record FaceMatch(UserAccount user, int distance) {
    }

    private int faceDistance(UserAccount user, String scannedSignature) {
        String storedSignature = resolveStoredFaceSignature(user);
        return faceVerificationService.distance(storedSignature, scannedSignature);
    }

    private FaceLoginCandidateResponse toCandidateResponse(FaceMatch match) {
        UserAccount user = match.user();
        return new FaceLoginCandidateResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarPath(),
                match.distance()
        );
    }

    private String resolveStoredFaceSignature(UserAccount user) {
        if (user == null) {
            return null;
        }

        if (user.getFaceTemplatePath() != null && !user.getFaceTemplatePath().isBlank()) {
            try {
                return faceVerificationService.generateSignatureFromStoredPath(user.getFaceTemplatePath());
            } catch (RuntimeException ex) {
                // Fall back to the stored hash if the template file is missing or unreadable.
                if (log.isDebugEnabled()) {
                    log.debug("Face template file could not be read for userId={}, username={}, path={}. Falling back to stored hash.",
                            user.getId(), user.getUsername(), user.getFaceTemplatePath(), ex);
                }
            }
        }
        return user.getFaceTemplateHash();
    }
}

