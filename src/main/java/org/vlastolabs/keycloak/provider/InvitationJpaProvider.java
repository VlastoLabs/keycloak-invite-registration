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
package org.vlastolabs.keycloak.provider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.KeycloakSession;
import org.vlastolabs.keycloak.entity.InvitationEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-based provider for managing invitation tokens in the database.
 * Uses Keycloak's JPA connection provider for transaction management.
 *
 * <p>This provider handles:
 * <ul>
 *   <li>Token lookup by token string or token + realm combination</li>
 *   <li>Token creation with configurable expiration</li>
 *   <li>Marking tokens as used after successful registration</li>
 * </ul>
 */
public class InvitationJpaProvider implements JpaEntityProvider, InvitationProvider {
    private static final Logger log = Logger.getLogger(InvitationJpaProvider.class);

    private static final int DEFAULT_EXPIRATION_SECONDS = 86400; // 24 hours
    private static final String NAMED_QUERY_BY_TOKEN = "findInviteByToken";
    private static final String NAMED_QUERY_BY_TOKEN_AND_REALM = "findInviteByTokenAndRealm";
    private static final String PARAM_TOKEN = "token";
    private static final String PARAM_REALM = "realm";

    private final KeycloakSession session;

    public InvitationJpaProvider(KeycloakSession session) {
        this.session = requireNonNull(session, "KeycloakSession cannot be null");
    }

    @Override
    public void close() {
        // No cleanup needed - EntityManager is managed by Keycloak
    }

    /**
     * Find an invitation by token.
     *
     * @param token The invitation token
     * @return Optional containing the InvitationEntity if found, empty otherwise
     * @throws IllegalArgumentException if token is null or blank
     */
    public Optional<InvitationEntity> findByToken(String token) {
        validateToken(token);
        return executeQuery(NAMED_QUERY_BY_TOKEN, query ->
                query.setParameter(PARAM_TOKEN, token)
        );
    }

    /**
     * Find an invitation by token and realm.
     *
     * @param token   The invitation token
     * @param realmId The realm ID
     * @return Optional containing the InvitationEntity if found, empty otherwise
     * @throws IllegalArgumentException if token or realmId is null or blank
     */
    public Optional<InvitationEntity> findByTokenAndRealm(String token, String realmId) {
        validateToken(token);
        validateRealmId(realmId);

        return executeQuery(NAMED_QUERY_BY_TOKEN_AND_REALM, query -> {
            query.setParameter(PARAM_TOKEN, token);
            query.setParameter(PARAM_REALM, realmId);
        });
    }

    // ==================== Creation Methods ====================

    /**
     * Create a new invitation token with default expiration (24 hours).
     *
     * @param realmId The realm for which to create the token
     * @return The generated token UUID as a string
     * @throws IllegalArgumentException if realmId is null or blank
     */
    public String createInvitation(String realmId) {
        return createInvitation(realmId, DEFAULT_EXPIRATION_SECONDS);
    }

    /**
     * Create a new invitation token with custom expiration.
     *
     * @param realmId           The realm for which to create the token
     * @param expirationSeconds Number of seconds until expiration (must be positive)
     * @return The generated token UUID as a string
     * @throws IllegalArgumentException if realmId is invalid or expirationSeconds is not positive
     */
    public String createInvitation(String realmId, int expirationSeconds) {
        validateRealmId(realmId);
        validateExpirationSeconds(expirationSeconds);

        var token = generateToken();
        var entity = createInvitationEntity(token, realmId, expirationSeconds);

        persistEntity(entity);
        logInvitationCreated(realmId, expirationSeconds);

        return token;
    }

    /**
     * Mark an invitation token as used.
     *
     * @param token   The invitation token to mark as used
     * @param realmId The realm ID for validation
     * @return true if token was found and marked as used, false if not found
     * @throws IllegalArgumentException if token or realmId is null or blank
     */
    public boolean markAsUsed(String token, String realmId) {
        return findByTokenAndRealm(token, realmId)
                .map(this::markEntityAsUsed)
                .isPresent();
    }

    // ==================== Private Helper Methods ====================

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private Optional<InvitationEntity> executeQuery(
            String queryName,
            QueryConfigurator configurator) {
        try {
            var em = getEntityManager();
            TypedQuery<InvitationEntity> query = em.createNamedQuery(queryName, InvitationEntity.class);
            configurator.configure(query);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.errorf(e, "Error executing query: %s", queryName);
            return Optional.empty();
        }
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    private InvitationEntity createInvitationEntity(
            String token,
            String realmId,
            int expirationSeconds) {
        var id = UUID.randomUUID().toString();
        var expiresOn = calculateExpirationTime(expirationSeconds);
        return new InvitationEntity(id, token, false, realmId, expiresOn);
    }

    private long calculateExpirationTime(int expirationSeconds) {
        return Instant.now().plusSeconds(expirationSeconds).toEpochMilli();
    }

    private void persistEntity(InvitationEntity entity) {
        getEntityManager().persist(entity);
    }

    private InvitationEntity markEntityAsUsed(InvitationEntity entity) {
        entity.setUsed(true);
        return getEntityManager().merge(entity);
    }

    // ==================== Validation Methods ====================

    private void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
    }

    private void validateRealmId(String realmId) {
        if (realmId == null || realmId.isBlank()) {
            throw new IllegalArgumentException("Realm ID cannot be null or blank");
        }
    }

    private void validateExpirationSeconds(int expirationSeconds) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Expiration seconds must be positive, got: " + expirationSeconds
            );
        }
    }

    private <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    private void logInvitationCreated(String realmId, int expirationSeconds) {
        if (log.isDebugEnabled()) {
            log.debugf("Created invitation for realm: %s, expires in: %d seconds",
                    realmId, expirationSeconds);
        }
    }

    @Override
    public List<Class<?>> getEntities() {
        return List.of(InvitationEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/invitation-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return "invitation-jpa-provider";
    }

    @FunctionalInterface
    private interface QueryConfigurator {
        void configure(TypedQuery<InvitationEntity> query);
    }
}
