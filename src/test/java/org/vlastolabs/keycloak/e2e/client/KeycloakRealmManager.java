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
package org.vlastolabs.keycloak.e2e.client;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;

public class KeycloakRealmManager {
    
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_CONFLICT = 409;
    
    private final String keycloakUrl;
    private final CloseableHttpClient httpClient;
    
    public KeycloakRealmManager(String keycloakUrl, CloseableHttpClient httpClient) {
        this.keycloakUrl = keycloakUrl;
        this.httpClient = httpClient;
    }

    public void createRealm(String realmName, String accessToken) throws IOException {
        HttpPost createRealmRequest = buildCreateRealmRequest(realmName, accessToken);
        
        try (ClassicHttpResponse response = httpClient.execute(createRealmRequest)) {
            validateCreationResponse(response, "Realm");
        }
    }

    public void createClient(String realmName, String clientId, String accessToken) throws IOException {
        HttpPost createClientRequest = buildCreateClientRequest(realmName, clientId, accessToken);
        
        try (ClassicHttpResponse response = httpClient.execute(createClientRequest)) {
            validateCreationResponse(response, "Client");
        }
    }
    
    private HttpPost buildCreateRealmRequest(String realmName, String accessToken) {
        HttpPost request = new HttpPost(keycloakUrl + "/admin/realms");
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
        
        String realmJson = String.format("""
            {
              "id": "%s",
              "realm": "%s",
              "enabled": true,
              "registrationAllowed": true,
              "registrationEmailAsUsername": true
            }
            """, realmName, realmName);
        
        request.setEntity(new StringEntity(realmJson, ContentType.APPLICATION_JSON));
        return request;
    }
    
    private HttpPost buildCreateClientRequest(String realmName, String clientId, String accessToken) {
        HttpPost request = new HttpPost(
                String.format("%s/admin/realms/%s/clients", keycloakUrl, realmName));
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
        
        String clientJson = String.format("""
            {
              "clientId": "%s",
              "name": "%s",
              "enabled": true,
              "publicClient": true,
              "redirectUris": ["http://localhost:*"],
              "webOrigins": ["+"],
              "protocol": "openid-connect"
            }
            """, clientId, clientId);
        
        request.setEntity(new StringEntity(clientJson, ContentType.APPLICATION_JSON));
        return request;
    }
    
    private void validateCreationResponse(ClassicHttpResponse response, String resourceType) {
        int statusCode = response.getCode();
        if (statusCode != HTTP_CREATED && statusCode != HTTP_CONFLICT) {
            throw new KeycloakResourceCreationException(
                    String.format("%s creation failed with status code: %d", resourceType, statusCode));
        }
    }

    public static class KeycloakResourceCreationException extends RuntimeException {
        public KeycloakResourceCreationException(String message) {
            super(message);
        }
    }
}
