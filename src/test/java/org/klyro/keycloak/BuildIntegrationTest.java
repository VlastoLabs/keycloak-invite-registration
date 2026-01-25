/*
 * Copyright 2026 Klyro Software
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
package org.klyro.keycloak;

import org.junit.jupiter.api.Test;
import org.klyro.keycloak.model.InviteGenerationResponse;

import static org.junit.jupiter.api.Assertions.*;

class BuildIntegrationTest {

    @Test
    void testInviteGenerationResponseCanBeCreated() {
        // This test verifies that the response model can be instantiated
        String token = "test-token";
        String realm = "test-realm";
        String message = "Test message";
        long expirationTime = System.currentTimeMillis() + 86400000; // 24 hours
        boolean used = false;

        InviteGenerationResponse response = new InviteGenerationResponse(
                token, realm, message, expirationTime, used
        );

        assertEquals(token, response.getToken());
        assertEquals(realm, response.getRealm());
        assertEquals(message, response.getMessage());
        assertEquals(expirationTime, response.getExpirationTime());
        assertEquals(used, response.isUsed());
    }
}