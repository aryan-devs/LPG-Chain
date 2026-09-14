package com.lpgchain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EmailPolicyProperties — Loads configurable approved email domains for
 * distributor organizations and administrators from application properties.
 */
@Component
public class EmailPolicyProperties {

    private final Set<String> distributorDomains;
    private final Set<String> adminDomains;

    public EmailPolicyProperties(
            @Value("${lpgchain.email.distributor-domains:distributor.example,agency.example,approved-distributor.example,lpgchain.demo}") String rawDistributorDomains,
            @Value("${lpgchain.email.admin-domains:lpgchain.example,admin.lpgchain.example,lpgchain.demo}") String rawAdminDomains) {

        this.distributorDomains = parseDomains(rawDistributorDomains);
        this.adminDomains = parseDomains(rawAdminDomains);
    }

    private Set<String> parseDomains(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> getDistributorDomains() {
        return distributorDomains;
    }

    public Set<String> getAdminDomains() {
        return adminDomains;
    }
}
