/*
 * Copyright 2026 VlastoLabs Software
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
/*
 * Copyright 2026 VlastoLabs Software
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
package org.vlastolabs.keycloak.service;

import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.vlastolabs.keycloak.entity.InvitationEntity;
import org.vlastolabs.keycloak.model.InviteGenerationResponse;
import org.vlastolabs.keycloak.model.ValidationResult;
import org.vlastolabs.keycloak.provider.InvitationProvider;

import java.util.Optional;

/**
 * Service layer for managing invitation tokens.
 */
public class InvitationService {
    private static final Logger log = Logger.getLogger(InvitationService.class);
    private static final int DEFAULT_EXPIRATION_SECONDS = 86400; // 24 hours

    private final KeycloakSession session;
    private final InvitationProvider provider;

    public InvitationService(KeycloakSession session) {
        this.session = requireNonNull(session, "KeycloakSession cannot be null");
        this.provider = (InvitationProvider) session.getProvider(
                JpaEntityProvider.class,
                "invitation-jpa-provider"
        );
        validateProvider();
    }

    /**
     * Constructor for testing purposes to inject provider dependency.
     */
    public InvitationService(InvitationProvider provider) {
        this.session = null; // Not used when provider is injected
        this.provider = requireNonNull(provider, "InvitationProvider cannot be null");
    }

    /**
     * Generate a new invitation token for the given realm with default expiration (24 hours).
     *
     * @param realmModel The realm for which to generate the token
     * @return InviteGenerationResponse containing the generated token
     * @throws IllegalArgumentException if realmModel is null
     */
    public InviteGenerationResponse generateInvite(RealmModel realmModel) {
        return generateInvite(realmModel, DEFAULT_EXPIRATION_SECONDS);
    }

    /**
     * Generate a new invitation token for the given realm with custom expiration.
     *
     * @param realmModel        The realm for which to generate the token
     * @param expirationSeconds Number of seconds until expiration
     * @return InviteGenerationResponse containing the generated token
     * @throws IllegalArgumentException      if realmModel is null or expirationSeconds is invalid
     * @throws InvitationGenerationException if token generation fails
     */
    public InviteGenerationResponse generateInvite(RealmModel realmModel, int expirationSeconds) {
        validateRealmModel(realmModel);

        String realmId = realmModel.getId();
        String token = provider.createInvitation(realmId, expirationSeconds);

        return provider.findByToken(token)
                .map(this::createInviteGenerationResponse)
                .orElseThrow(() -> new InvitationGenerationException(
                        "Failed to retrieve newly created invitation token: " + token
                ));
    }

    private InviteGenerationResponse createInviteGenerationResponse(InvitationEntity entity) {
        return new InviteGenerationResponse(
                entity.getToken(),
                entity.getRealm(),
                "Invitation token generated successfully",
                entity.getExpiresOn(),
                entity.isUsed()
        );
    }

    /**
     * Validate an invitation token and return detailed validation result.
     *
     * @param token The token to validate
     * @return ValidationResult containing the invitation entity if valid, or error code if invalid
     */
    public ValidationResult validateInviteDetailed(String token) {
        if (!isValidTokenFormat(token)) {
            return ValidationResult.missingToken();
        }

        return provider.findByToken(token)
                .map(this::validateEntity)
                .orElse(ValidationResult.invalidToken());
    }

    /**
     * Validate an invitation token for a specific realm and return detailed validation result.
     *
     * @param token   The token to validate
     * @param realmId The realm ID to validate against
     * @return ValidationResult containing the invitation entity if valid, or error code if invalid
     */
    public ValidationResult validateInviteDetailed(String token, String realmId) {
        if (!isValidTokenFormat(token) || !isValidRealmId(realmId)) {
            return ValidationResult.missingToken();
        }

        return provider.findByTokenAndRealm(token, realmId)
                .map(this::validateEntity)
                .orElse(ValidationResult.invalidToken());
    }

    /**
     * Validate an invitation token.
     *
     * @param token The token to validate
     * @return Optional containing the invitation entity if valid and unused, empty otherwise
     */
    public Optional<InvitationEntity> validateInvite(String token) {
        return validateInviteDetailed(token).getInvitationEntity();
    }

    /**
     * Validate an invitation token for a specific realm.
     *
     * @param token   The token to validate
     * @param realmId The realm ID to validate against
     * @return Optional containing the invitation entity if valid and unused, empty otherwise
     */
    public Optional<InvitationEntity> validateInvite(String token, String realmId) {
        return validateInviteDetailed(token, realmId).getInvitationEntity();
    }

    private ValidationResult validateEntity(InvitationEntity entity) {
        if (entity.isUsed()) {
            return ValidationResult.usedToken();
        }

        if (isExpired(entity)) {
            return ValidationResult.expiredToken();
        }

        return ValidationResult.valid(entity);
    }

    /**
     * Mark an invitation token as used.
     *
     * @param token The token to mark as used
     */
    public void markAsUsed(String token) {
        if (!isValidTokenFormat(token)) {
            logWarning("Attempted to mark invalid token as used: " + token);
            return;
        }

        provider.findByToken(token)
                .ifPresentOrElse(
                        entity -> markEntityAsUsed(token, entity),
                        () -> logWarning("Attempted to mark non-existent token as used: " + token)
                );
    }

    private void markEntityAsUsed(String token, InvitationEntity entity) {
        boolean marked = provider.markAsUsed(token, entity.getRealm());
        if (marked) {
            logDebug("Successfully marked token as used: " + token);
        } else {
            logWarning("Failed to mark token as used: " + token);
        }
    }

    private boolean isValidTokenFormat(String token) {
        return token != null && !token.isBlank();
    }

    private boolean isValidRealmId(String realmId) {
        return realmId != null && !realmId.isBlank();
    }

    private boolean isExpired(InvitationEntity entity) {
        return entity.getExpiresOn() != null &&
                entity.getExpiresOn() < System.currentTimeMillis();
    }

    private void validateRealmModel(RealmModel realmModel) {
        if (realmModel == null) {
            throw new IllegalArgumentException("RealmModel cannot be null");
        }
    }

    private void validateProvider() {
        if (provider == null) {
            throw new IllegalStateException(
                    "InvitationProvider not found - ensure the provider is properly registered"
            );
        }
    }

    private <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    private void logDebug(String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
    }

    private void logWarning(String message) {
        log.warn(message);
    }

    public static class InvitationGenerationException extends RuntimeException {
        public InvitationGenerationException(String message) {
            super(message);
        }

        public InvitationGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}