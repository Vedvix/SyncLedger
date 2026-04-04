package com.vedvix.syncledger.config;

import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.repository.SubscriptionRepository;
import com.vedvix.syncledger.repository.UserRepository;
import com.vedvix.syncledger.service.EncryptionService;
import com.vedvix.syncledger.service.SubscriptionService;
import com.vedvix.syncledger.service.erp.ErpPropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Startup initializer that seeds predefined organizations on application start.
 * Idempotent — skips creation if the organization already exists (by slug).
 * All sensitive credentials are read from environment variables / application properties.
 *
 * Required env vars for LongHome seed:
 *   SEED_LONGHOME_ENABLED          (default: true)
 *   SEED_LONGHOME_MS_CLIENT_ID
 *   SEED_LONGHOME_MS_TENANT_ID
 *   SEED_LONGHOME_MS_CLIENT_SECRET
 *   SEED_LONGHOME_MS_MAILBOX
 *   SEED_LONGHOME_ERP_COMPANY_ID
 *   SEED_LONGHOME_ERP_USER_ID
 *   SEED_LONGHOME_ERP_PASSWORD
 *   SEED_LONGHOME_OBJECT_ID
 *   SEED_LONGHOME_CERT_SECRET_ID
 *   SEED_LONGHOME_ADMIN_PASSWORD
 *
 * @author vedvix
 */
@Slf4j
@Component
@Order(1)
public class OrganizationInitializer implements ApplicationRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final EncryptionService encryptionService;
    private final PasswordEncoder passwordEncoder;
    private final ErpPropertyService erpPropertyService;

    // ── LongHome seed configuration (from env / properties) ────────
    @Value("${seed.longhome.enabled:true}")
    private boolean seedEnabled;

    @Value("${seed.longhome.ms-client-id:#{null}}")
    private String msClientId;

    @Value("${seed.longhome.ms-tenant-id:#{null}}")
    private String msTenantId;

    @Value("${seed.longhome.ms-client-secret:#{null}}")
    private String msClientSecret;

    @Value("${seed.longhome.ms-mailbox:admin@evolotek.ai}")
    private String msMailboxEmail;

    @Value("${seed.longhome.erp-company-id:#{null}}")
    private String erpCompanyId;

    @Value("${seed.longhome.erp-user-id:#{null}}")
    private String erpUserId;

    @Value("${seed.longhome.erp-password:#{null}}")
    private String erpPassword;

    @Value("${seed.longhome.erp-sender-id:#{null}}")
    private String erpSenderId;

    @Value("${seed.longhome.erp-sender-password:#{null}}")
    private String erpSenderPassword;

    @Value("${seed.longhome.object-id:#{null}}")
    private String objectId;

    @Value("${seed.longhome.cert-secret-id:#{null}}")
    private String certSecretId;

    @Value("${seed.longhome.admin-password:LongHome@2026!}")
    private String adminPassword;

    public OrganizationInitializer(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionService subscriptionService,
            EncryptionService encryptionService,
            PasswordEncoder passwordEncoder,
            ErpPropertyService erpPropertyService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.encryptionService = encryptionService;
        this.passwordEncoder = passwordEncoder;
        this.erpPropertyService = erpPropertyService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seedEnabled) {
            seedLongHomeOrganization();
        } else {
            log.info("Organization seed disabled (seed.longhome.enabled=false)");
        }
    }

    /**
     * Seeds the LongHome organization with full Microsoft, ERP (Sage Intacct),
     * admin user, and trial subscription configuration.
     */
    private void seedLongHomeOrganization() {
        final String slug = "longhome";
        final String orgName = "LongHome";

        // ── Idempotency check ──────────────────────────────────────────
        if (organizationRepository.existsBySlug(slug)) {
            log.info("✅ Organization '{}' already exists — skipping seed.", orgName);
            return;
        }

        // ── Validate required credentials ──────────────────────────────
        if (msClientId == null || msClientSecret == null || msTenantId == null) {
            log.warn("⚠️  LongHome seed skipped — Microsoft credentials not configured. " +
                     "Set SEED_LONGHOME_MS_CLIENT_ID, SEED_LONGHOME_MS_TENANT_ID, SEED_LONGHOME_MS_CLIENT_SECRET");
            return;
        }
        if (erpCompanyId == null || erpUserId == null || erpPassword == null) {
            log.warn("⚠️  LongHome seed skipped — Sage Intacct credentials not configured. " +
                     "Set SEED_LONGHOME_ERP_COMPANY_ID, SEED_LONGHOME_ERP_USER_ID, SEED_LONGHOME_ERP_PASSWORD");
            return;
        }

        log.info("🔧 Seeding organization: {} ...", orgName);

        // ── 1. Create Organization ─────────────────────────────────────
        Organization org = Organization.builder()
                .name(orgName)
                .slug(slug)
                .emailAddress(msMailboxEmail)
                .status(OrganizationStatus.ACTIVE)
                .contactName("LongHome Admin")
                .contactEmail(msMailboxEmail)
                // Microsoft Graph config
                .msClientId(msClientId)
                .msClientSecretEncrypted(encryptionService.encrypt(msClientSecret))
                .msTenantId(msTenantId)
                .msMailboxEmail(msMailboxEmail)
                .msCredentialsVerified(true)
                .msCredentialsVerifiedAt(LocalDateTime.now())
                // ERP type (properties stored in erp_properties table)
                .erpType(ErpType.SAGE)
                .erpAutoSync(true)
                // AWS paths
                .s3FolderPath("organizations/" + slug + "/invoices")
                .sqsQueueName("syncledger-" + slug + "-queue")
                .build();

        org = organizationRepository.save(org);
        log.info("   ✅ Organization created: {} (id={})", org.getName(), org.getId());

        // ── 1b. Seed Sage Intacct ERP properties into generic table ────
        Map<String, String> sageProps = new LinkedHashMap<>();
        sageProps.put("company_id", erpCompanyId);
        sageProps.put("user_id", erpUserId);
        sageProps.put("user_password", erpPassword);
        if (erpSenderId != null) sageProps.put("sender_id", erpSenderId);
        if (erpSenderPassword != null) sageProps.put("sender_password", erpSenderPassword);
        sageProps.put("gateway_url", "https://api.intacct.com/ia/xml/xmlgw.phtml");
        sageProps.put("auto_sync", "true");
        erpPropertyService.saveProperties(org.getId(), ErpType.SAGE, sageProps);
        log.info("   ✅ Sage Intacct ERP properties seeded ({} keys)", sageProps.size());

        // ── 2. Create Admin User (if not exists) ───────────────────────
        if (!userRepository.existsByEmailIgnoreCase(msMailboxEmail)) {
            User adminUser = User.builder()
                    .organization(org)
                    .firstName("LongHome")
                    .lastName("Admin")
                    .email(msMailboxEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ADMIN)
                    .isActive(true)
                    .build();
            adminUser = userRepository.save(adminUser);
            log.info("   ✅ Admin user created: {} (id={})", adminUser.getEmail(), adminUser.getId());
        } else {
            log.info("   ℹ️  Admin user '{}' already exists — skipping.", msMailboxEmail);
        }

        // ── 3. Create Trial Subscription (if not exists) ──────────────
        if (subscriptionRepository.findByOrganization_Id(org.getId()).isEmpty()) {
            Subscription subscription = subscriptionService.createTrialSubscription(org);
            log.info("   ✅ Trial subscription created (expires: {})", subscription.getTrialEndDate());
        } else {
            log.info("   ℹ️  Subscription already exists for org — skipping.");
        }

        log.info("🎉 Organization '{}' seeded successfully!", orgName);
        log.info("   📧 Mailbox: {}", msMailboxEmail);
        log.info("   🏢 ERP: Sage Intacct (company: {})", erpCompanyId);
        log.info("   🔑 Admin login: {}", msMailboxEmail);
    }
}
