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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class InvitationJpaProvider implements JpaEntityProvider, InvitationProvider {

    private static final Logger log = Logger.getLogger(InvitationJpaProvider.class);
    private static final int DEFAULT_EXPIRATION_SECONDS = 86400;
    private static final String QUERY_BY_TOKEN = "findInviteByToken";
    private static final String QUERY_BY_TOKEN_AND_REALM = "findInviteByTokenAndRealm";
    private static final String PARAM_TOKEN = "token";
    private static final String PARAM_REALM = "realm";

    private final KeycloakSession session;

    public InvitationJpaProvider(KeycloakSession session) {
        this.session = Objects.requireNonNull(session, "KeycloakSession cannot be null");
    }

    @Override
    public void close() {
    }

    public Optional<InvitationEntity> findByToken(String token) {
        validateToken(token);
        return executeQuery(QUERY_BY_TOKEN, query -> query.setParameter(PARAM_TOKEN, token));
    }

    public Optional<InvitationEntity> findByTokenAndRealm(String token, String realmId) {
        validateToken(token);
        validateRealmId(realmId);

        return executeQuery(QUERY_BY_TOKEN_AND_REALM, query -> {
            query.setParameter(PARAM_TOKEN, token);
            query.setParameter(PARAM_REALM, realmId);
        });
    }

    public String createInvitation(String realmId) {
        return createInvitation(realmId, DEFAULT_EXPIRATION_SECONDS);
    }

    public String createInvitation(String realmId, int expirationSeconds) {
        validateRealmId(realmId);
        validateExpirationSeconds(expirationSeconds);

        var token = generateToken();
        var entity = buildInvitationEntity(token, realmId, expirationSeconds);

        entityManager().persist(entity);
        logCreation(realmId, expirationSeconds);

        return token;
    }

    public boolean markAsUsed(String token, String realmId) {
        return findByTokenAndRealm(token, realmId)
                .map(this::markAsUsed)
                .isPresent();
    }

    @Override
    public List<InvitationEntity> findAll(int offset, int limit) {
        var query = entityManager().createQuery(
                "SELECT i FROM InvitationEntity i ORDER BY i.createdOn DESC",
                InvitationEntity.class
        );
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public long countAll() {
        return entityManager()
                .createQuery("SELECT COUNT(i) FROM InvitationEntity i", Long.class)
                .getSingleResult();
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

    private EntityManager entityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private Optional<InvitationEntity> executeQuery(String queryName, QueryConfigurator configurator) {
        try {
            var query = entityManager().createNamedQuery(queryName, InvitationEntity.class);
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

    private InvitationEntity buildInvitationEntity(String token, String realmId, int expirationSeconds) {
        return new InvitationEntity(
                UUID.randomUUID().toString(),
                token,
                false,
                realmId,
                Instant.now().plusSeconds(expirationSeconds).toEpochMilli()
        );
    }

    private InvitationEntity markAsUsed(InvitationEntity entity) {
        entity.setUsed(true);
        return entityManager().merge(entity);
    }

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
            throw new IllegalArgumentException("Expiration seconds must be positive, got: " + expirationSeconds);
        }
    }

    private void logCreation(String realmId, int expirationSeconds) {
        if (log.isDebugEnabled()) {
            log.debugf("Created invitation for realm: %s, expires in: %d seconds", realmId, expirationSeconds);
        }
    }

    @FunctionalInterface
    private interface QueryConfigurator {
        void configure(TypedQuery<InvitationEntity> query);
    }
}