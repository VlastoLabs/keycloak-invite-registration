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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.RealmModel;
import org.klyro.keycloak.entity.InvitationEntity;
import org.klyro.keycloak.model.InviteGenerationResponse;
import org.klyro.keycloak.provider.InvitationProvider;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InvitationServiceTest {

    @Mock
    private InvitationProvider provider;

    @Mock
    private RealmModel realmModel;

    private InvitationService invitationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        invitationService = new InvitationService(provider);
    }

    @Test
    void generateInvite_shouldReturnValidResponse() {
        // Arrange
        String expectedToken = "test-token";
        String realmId = "test-realm";
        when(realmModel.getId()).thenReturn(realmId);
        when(provider.createInvitation(eq(realmId), anyInt())).thenReturn(expectedToken);

        InvitationEntity entity = new InvitationEntity("id", expectedToken, false, realmId, System.currentTimeMillis() + 86400000L);
        when(provider.findByToken(expectedToken)).thenReturn(entity);

        // Act
        InviteGenerationResponse response = invitationService.generateInvite(realmModel);

        // Assert
        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        assertEquals(realmId, response.getRealm());
        assertEquals("Invitation token generated successfully", response.getMessage());
        assertFalse(response.isUsed());
        assertTrue(response.getExpirationTime() > 0);
    }

    @Test
    void validateInvite_withValidUnusedToken_shouldReturnEntity() {
        // Arrange
        String token = "valid-token";
        InvitationEntity entity = new InvitationEntity("id", token, false, "realm");
        when(provider.findByToken(token)).thenReturn(entity);

        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite(token);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void validateInvite_withNullToken_shouldReturnEmpty() {
        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite(null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateInvite_withEmptyToken_shouldReturnEmpty() {
        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite("");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateInvite_withUsedToken_shouldReturnEmpty() {
        // Arrange
        String token = "used-token";
        InvitationEntity entity = new InvitationEntity("id", token, true, "realm"); // isUsed = true
        when(provider.findByToken(token)).thenReturn(entity);

        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite(token);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateInvite_withNonExistentToken_shouldReturnEmpty() {
        // Arrange
        String token = "non-existent-token";
        when(provider.findByToken(token)).thenReturn(null);

        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite(token);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateInvite_withRealm_withValidUnusedToken_shouldReturnEntity() {
        // Arrange
        String token = "valid-token";
        String realmId = "test-realm";
        InvitationEntity entity = new InvitationEntity("id", token, false, realmId);
        when(provider.findByTokenAndRealm(token, realmId)).thenReturn(entity);

        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite(token, realmId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    void validateInvite_withRealm_withNullToken_shouldReturnEmpty() {
        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite(null, "realm");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateInvite_withRealm_withEmptyToken_shouldReturnEmpty() {
        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite("", "realm");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateInvite_withRealm_withNullRealm_shouldReturnEmpty() {
        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite("token", null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateInvite_withRealm_withEmptyRealm_shouldReturnEmpty() {
        // Act
        Optional<InvitationEntity> result = invitationService.validateInvite("token", "");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void markAsUsed_withValidToken_shouldCallProvider() {
        // Arrange
        String token = "test-token";

        // Act
        invitationService.markAsUsed(token);

        // Assert
        verify(provider).markAsUsed(token);
    }

    @Test
    void markAsUsed_withNullToken_shouldNotCallProvider() {
        // Act
        invitationService.markAsUsed(null);

        // Assert
        verify(provider, never()).markAsUsed(anyString());
    }

    @Test
    void markAsUsed_withEmptyToken_shouldNotCallProvider() {
        // Act
        invitationService.markAsUsed("");

        // Assert
        verify(provider, never()).markAsUsed(anyString());
    }
}