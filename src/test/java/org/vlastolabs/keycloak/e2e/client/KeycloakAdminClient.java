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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.Map;

public class KeycloakAdminClient {

    private final String keycloakUrl;
    private final String adminUsername;
    private final String adminPassword;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KeycloakAdminClient(String keycloakUrl, String adminUsername, String adminPassword,
                               CloseableHttpClient httpClient, ObjectMapper objectMapper) {
        this.keycloakUrl = keycloakUrl;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String authenticate() throws IOException {
        HttpPost tokenRequest = createTokenRequest();

        try (ClassicHttpResponse response = httpClient.execute(tokenRequest)) {
            if (response.getCode() != 200) {
                throw new KeycloakAuthenticationException(
                        "Admin authentication failed with status code: " + response.getCode());
            }
            return extractAccessToken(response);
        }
    }

    private HttpPost createTokenRequest() {
        HttpPost request = new HttpPost(keycloakUrl + "/realms/master/protocol/openid-connect/token");
        request.setHeader("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.toString());

        String body = String.format("grant_type=password&client_id=admin-cli&username=%s&password=%s",
                adminUsername, adminPassword);

        request.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));
        return request;
    }

    private String extractAccessToken(ClassicHttpResponse response) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = objectMapper.readValue(
                response.getEntity().getContent(), Map.class);

        String accessToken = (String) tokenResponse.get("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new KeycloakAuthenticationException("Access token not found in response");
        }

        return accessToken;
    }

    public static class KeycloakAuthenticationException extends RuntimeException {
        public KeycloakAuthenticationException(String message) {
            super(message);
        }
    }
}