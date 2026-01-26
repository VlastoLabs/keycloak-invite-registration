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
package org.vlastolabs.keycloak.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * JPA Entity for storing invitation tokens.
 * Each token can be used only once (used flag).
 */
@Entity
@Table(name = "CUSTOM_INVITATION")
@NamedQueries({
        @NamedQuery(
                name = "findInviteByToken",
                query = "SELECT i FROM InvitationEntity i WHERE i.token = :token"
        ),
        @NamedQuery(
                name = "findInviteByTokenAndRealm",
                query = "SELECT i FROM InvitationEntity i WHERE i.token = :token AND i.realm = :realm"
        )
})
public class InvitationEntity {
    @Id
    @Column(name = "ID")
    @JsonProperty("id")
    private String id;

    @Column(name = "TOKEN", unique = true, nullable = false)
    @JsonProperty("token")
    private String token;

    @Column(name = "IS_USED")
    @JsonProperty("used")
    private boolean used;

    @Column(name = "REALM")
    @JsonProperty("realm")
    private String realm;

    @Column(name = "CREATED_ON")
    @JsonProperty("createdOn")
    private long createdOn;

    @Column(name = "EXPIRES_ON")
    @JsonProperty("expiresOn")
    private Long expiresOn;

    // Default constructor required by JPA
    public InvitationEntity() {
    }

    public InvitationEntity(String id, String token, boolean used, String realm) {
        this.id = id;
        this.token = token;
        this.used = used;
        this.realm = realm;
        this.createdOn = Instant.now().toEpochMilli();
    }

    public InvitationEntity(String id, String token, boolean used, String realm, Long expiresOn) {
        this.id = id;
        this.token = token;
        this.used = used;
        this.realm = realm;
        this.createdOn = Instant.now().toEpochMilli();
        this.expiresOn = expiresOn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public long getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(long createdOn) {
        this.createdOn = createdOn;
    }

    public Long getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(Long expiresOn) {
        this.expiresOn = expiresOn;
    }
}