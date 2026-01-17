/*
 * Copyright 2022 Klyro
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.klyro.keycloak.service;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.klyro.keycloak.entity.InvitationEntity;
import org.klyro.keycloak.model.InviteGenerationResponse;
import org.klyro.keycloak.provider.InvitationJpaProvider;
import org.klyro.keycloak.provider.InvitationProvider;

import java.util.Optional;

/**
 * Service layer for managing invitation tokens.
 * Provides business logic for invitation token operations.
 */
public class InvitationService {
    private final KeycloakSession session;
    private final InvitationProvider provider;

    public InvitationService(KeycloakSession session) {
        this.session = session;
        this.provider = (InvitationProvider) session.getProvider(JpaEntityProvider.class, "invitation-jpa-provider");
    }

    /**
     * Constructor for testing purposes to inject provider dependency.
     */
    public InvitationService(InvitationProvider provider) {
        this.session = null; // Not used when provider is injected
        this.provider = provider;
    }

    /**
     * Generate a new invitation token for the given realm with default expiration (24 hours).
     *
     * @param realmModel The realm for which to generate the token
     * @return InviteGenerationResponse containing the generated token
     */
    public InviteGenerationResponse generateInvite(RealmModel realmModel) {
        return generateInvite(realmModel, 86400); // 24 hours in seconds
    }

    /**
     * Generate a new invitation token for the given realm with custom expiration.
     *
     * @param realmModel The realm for which to generate the token
     * @param expirationSeconds Number of seconds until expiration
     * @return InviteGenerationResponse containing the generated token
     */
    public InviteGenerationResponse generateInvite(RealmModel realmModel, int expirationSeconds) {
        String token = provider.createInvitation(realmModel.getId(), expirationSeconds);

        InvitationEntity entity = provider.findByToken(token);
        if (entity != null) {
            return new InviteGenerationResponse(
                    token,
                    realmModel.getId(),
                    "Invitation token generated successfully",
                    entity.getExpiresOn() != null ? entity.getExpiresOn() : 0,
                    entity.isUsed()
            );
        } else {
            // This shouldn't happen, but just in case
            return new InviteGenerationResponse(
                    token,
                    realmModel.getId(),
                    "Invitation token generated successfully",
                    System.currentTimeMillis() + (expirationSeconds * 1000L),
                    false
            );
        }
    }

    /**
     * Validate an invitation token.
     *
     * @param token The token to validate
     * @return Optional containing the invitation entity if valid and unused, empty otherwise
     */
    public Optional<InvitationEntity> validateInvite(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }

        InvitationEntity invitation = provider.findByToken(token);

        if (invitation != null && !invitation.isUsed()) {
            return Optional.of(invitation);
        }

        return Optional.empty();
    }

    /**
     * Validate an invitation token for a specific realm.
     *
     * @param token The token to validate
     * @param realmId The realm ID to validate against
     * @return Optional containing the invitation entity if valid and unused, empty otherwise
     */
    public Optional<InvitationEntity> validateInvite(String token, String realmId) {
        if (token == null || token.isEmpty() || realmId == null || realmId.isEmpty()) {
            return Optional.empty();
        }

        InvitationEntity invitation = provider.findByTokenAndRealm(token, realmId);

        if (invitation != null && !invitation.isUsed()) {
            return Optional.of(invitation);
        }

        return Optional.empty();
    }

    /**
     * Mark an invitation token as used.
     *
     * @param token The token to mark as used
     */
    public void markAsUsed(String token) {
        if (token != null && !token.isEmpty()) {
            provider.markAsUsed(token);
        }
    }

    /**
     * Find an invitation by token.
     *
     * @param token The token to search for
     * @return The invitation entity if found, null otherwise
     */
    public InvitationEntity findByToken(String token) {
        return provider.findByToken(token);
    }
}