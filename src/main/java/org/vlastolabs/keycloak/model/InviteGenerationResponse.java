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
package org.vlastolabs.keycloak.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for invitation token generation.
 * Contains the generated token and associated metadata.
 */
public class InviteGenerationResponse {
    private final String token;
    private final String realm;
    private final String message;
    private final long expirationTime;
    private final boolean used;

    public InviteGenerationResponse(String token, String realm, String message, long expirationTime, boolean used) {
        this.token = token;
        this.realm = realm;
        this.message = message;
        this.expirationTime = expirationTime;
        this.used = used;
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    @JsonProperty("realm")
    public String getRealm() {
        return realm;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("expirationTime")
    public long getExpirationTime() {
        return expirationTime;
    }

    @JsonProperty("used")
    public boolean isUsed() {
        return used;
    }
}