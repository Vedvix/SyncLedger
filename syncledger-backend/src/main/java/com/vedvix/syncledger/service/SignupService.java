package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.UserRepository;
import com.vedvix.syncledger.security.JwtTokenProvider;
import com.vedvix.syncledger.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Service handling organization self-signup flow.
 * Creates the organization, admin user, and trial subscription in one atomic transaction.
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignupService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * Process organization self-signup.
     * Creates org + admin user + trial subscription in a single transaction.
     * 
     * @param request Signup request with org and admin details
     * @param userAgent User agent for session tracking
     * @param ipAddress IP address for session tracking
     * @return Signup response with auth tokens
     */
    @Transactional
    public SignupResponse signup(OrganizationSignupRequest request, String userAgent, String ipAddress) {
        log.info("Processing organization signup: {}", request.getOrganizationName());

        // Validate uniqueness
        if (userRepository.existsByEmailIgnoreCase(request.getAdminEmail())) {
            throw new BadRequestException("Email address is already registered");
        }
        if (organizationRepository.existsByEmailAddress(request.getOrganizationEmail())) {
            throw new BadRequestException("Organization email is already registered");
        }

        // Generate slug from org name
        String slug = toSlug(request.getOrganizationName());
        int attempt = 0;
        String candidateSlug = slug;
        while (organizationRepository.existsBySlug(candidateSlug)) {
            attempt++;
            candidateSlug = slug + "-" + attempt;
        }
        slug = candidateSlug;

        // 1. Create Organization (starts in ONBOARDING until setup is complete)
        Organization org = Organization.builder()
                .name(request.getOrganizationName())
                .slug(slug)
                .emailAddress(request.getOrganizationEmail())
                .status(OrganizationStatus.ONBOARDING)
                .contactName(request.getFirstName() + " " + request.getLastName())
                .contactEmail(request.getAdminEmail())
                .contactPhone(request.getPhone())
                .s3FolderPath("organizations/" + slug + "/invoices")
                .sqsQueueName("syncledger-" + slug + "-queue")
                .build();

        org = organizationRepository.save(org);
        log.info("Organization created via signup: {} (slug: {})", org.getName(), org.getSlug());

        // 2. Create Admin User
        User adminUser = User.builder()
                .organization(org)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getAdminEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        adminUser = userRepository.save(adminUser);
        log.info("Admin user created for org {}: {}", org.getName(), adminUser.getEmail());

        // 3. Create Trial Subscription
        Subscription subscription = subscriptionService.createTrialSubscription(org);

        // 4. Generate auth tokens
        UserPrincipal userPrincipal = UserPrincipal.create(adminUser);
        String accessToken = tokenProvider.generateToken(userPrincipal);
        String refreshToken = refreshTokenService.createRefreshToken(adminUser, userAgent, ipAddress);

        UserDTO userDTO = UserDTO.builder()
                .id(adminUser.getId())
                .email(adminUser.getEmail())
                .firstName(adminUser.getFirstName())
                .lastName(adminUser.getLastName())
                .role(adminUser.getRole().name())
                .organizationId(org.getId())
                .organizationSlug(org.getSlug())
                .organizationName(org.getName())
                .organizationStatus(org.getStatus().name())
                .isActive(true)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(userDTO)
                .build();

        // 5. Send welcome email (async)
        try {
            emailService.sendTrialWelcomeEmail(org, request.getAdminEmail(),
                    request.getFirstName() + " " + request.getLastName());

            // Notify super admins about new signup
            notifySuperAdmins(org);
        } catch (Exception e) {
            log.warn("Failed to send signup emails (non-blocking): {}", e.getMessage());
        }

        OrganizationDTO orgDTO = OrganizationDTO.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .emailAddress(org.getEmailAddress())
                .status(org.getStatus().name())
                .hasAccess(true)
                .build();

        SubscriptionDTO subDTO = subscriptionService.mapToDTO(subscription);

        log.info("Signup completed successfully for org: {}", org.getName());

        return SignupResponse.builder()
                .organization(orgDTO)
                .subscription(subDTO)
                .auth(authResponse)
                .message("Welcome! Your 15-day free trial has started.")
                .build();
    }

    /**
     * Notify all super admins about a new organization signup.
     */
    private void notifySuperAdmins(Organization org) {
        List<User> superAdmins = userRepository.findByRole(UserRole.SUPER_ADMIN);
        for (User admin : superAdmins) {
            emailService.sendNewOrgNotificationToAdmin(admin.getEmail(), org);
        }
    }

    /**
     * Generate URL-friendly slug from organization name.
     */
    private String toSlug(String input) {
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH)
                   .replaceAll("-{2,}", "-")
                   .replaceAll("^-|-$", "");
    }
}
