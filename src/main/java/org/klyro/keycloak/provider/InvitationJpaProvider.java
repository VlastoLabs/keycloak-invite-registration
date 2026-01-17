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
package org.klyro.keycloak.provider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.klyro.keycloak.entity.InvitationEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Provider for managing invitation tokens in the database.
 * Uses Keycloak's JPA connection provider for transaction management.
 */
public class InvitationJpaProvider implements JpaEntityProvider, InvitationProvider {
    private final KeycloakSession session;

    public InvitationJpaProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        // No cleanup needed
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    /**
     * Find an invitation by token.
     * @param token The invitation token
     * @return InvitationEntity if found and not expired, null otherwise
     */
    public InvitationEntity findByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            TypedQuery<InvitationEntity> query = getEntityManager()
                    .createNamedQuery("findInviteByToken", InvitationEntity.class);
            query.setParameter("token", token);
            InvitationEntity entity = query.getSingleResult();

            // Check if the invitation has expired
            if (entity != null && isExpired(entity)) {
                // Mark as used if expired
                entity.setUsed(true);
                getEntityManager().merge(entity);
                return null;
            }

            return entity;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Find an invitation by token and realm.
     * @param token The invitation token
     * @param realm The realm ID
     * @return InvitationEntity if found and not expired, null otherwise
     */
    public InvitationEntity findByTokenAndRealm(String token, String realm) {
        if (token == null || token.isEmpty() || realm == null || realm.isEmpty()) {
            return null;
        }

        try {
            TypedQuery<InvitationEntity> query = getEntityManager()
                    .createNamedQuery("findInviteByTokenAndRealm", InvitationEntity.class);
            query.setParameter("token", token);
            query.setParameter("realm", realm);
            InvitationEntity entity = query.getSingleResult();

            // Check if the invitation has expired
            if (entity != null && isExpired(entity)) {
                // Mark as used if expired
                entity.setUsed(true);
                getEntityManager().merge(entity);
                return null;
            }

            return entity;
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Create a new invitation token with default expiration (24 hours).
     * @param realm The realm for which to create the token
     * @return The generated token UUID as a string
     */
    public String createInvitation(String realm) {
        return createInvitation(realm, 86400); // 24 hours in seconds
    }

    /**
     * Create a new invitation token with custom expiration.
     * @param realm The realm for which to create the token
     * @param expirationSeconds Number of seconds until expiration
     * @return The generated token UUID as a string
     */
    public String createInvitation(String realm, int expirationSeconds) {
        String token = UUID.randomUUID().toString();
        String id = UUID.randomUUID().toString();
        long expiresOn = Instant.now().plusSeconds(expirationSeconds).toEpochMilli();

        InvitationEntity entity = new InvitationEntity(id, token, false, realm, expiresOn);
        getEntityManager().persist(entity);

        return token;
    }

    /**
     * Mark an invitation token as used.
     * @param token The invitation token to mark as used
     */
    public void markAsUsed(String token) {
        InvitationEntity entity = findByToken(token);
        if (entity != null) {
            entity.setUsed(true);
            getEntityManager().merge(entity);
        }
    }

    /**
     * Check if an invitation entity has expired.
     * @param entity The invitation entity to check
     * @return true if expired, false otherwise
     */
    private boolean isExpired(InvitationEntity entity) {
        if (entity.getExpiresOn() == null) {
            return false; // No expiration set
        }
        return Instant.now().toEpochMilli() > entity.getExpiresOn();
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
}

