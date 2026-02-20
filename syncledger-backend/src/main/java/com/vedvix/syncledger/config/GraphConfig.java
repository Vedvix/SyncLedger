package com.vedvix.syncledger.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * Microsoft Graph API configuration for email integration.
 * Uses Azure AD App Registration with client credentials flow.
 * 
 * Prerequisites:
 * 1. Register an app in Azure AD
 * 2. Grant Mail.Read application permission
 * 3. Grant admin consent
 * 4. Configure the credentials in environment variables
 * 
 * @author vedvix
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "email.polling.enabled", havingValue = "true")
public class GraphConfig {

    @Value("${microsoft.graph.client-id}")
    private String clientId;

    @Value("${microsoft.graph.client-secret}")
    private String clientSecret;

    @Value("${microsoft.graph.tenant-id}")
    private String tenantId;

    /**
     * Create Azure AD credentials for Graph API authentication.
     */
    @Bean
    public ClientSecretCredential clientSecretCredential() {
        log.info("Initializing Azure AD credentials for tenant: {}", tenantId);
        
        return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();
    }

    /**
     * Create RestTemplate configured for Microsoft Graph API calls.
     */
    @Bean(name = "graphRestTemplate")
    public RestTemplate graphRestTemplate() {
        // Use JdkClientHttpRequestFactory to support PATCH method
        // (default SimpleClientHttpRequestFactory doesn't support PATCH)
        return new RestTemplate(new JdkClientHttpRequestFactory());
    }
}
