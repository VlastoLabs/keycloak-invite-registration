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

import org.vlastolabs.keycloak.entity.InvitationEntity;

import java.util.Optional;

/**
 * Result of invitation validation containing the entity (if valid) and error code (if invalid).
 */
public record ValidationResult(
        InvitationEntity invitationEntity,
        String errorCode,
        boolean isValid
) {
    // Compact constructor for validation
    public ValidationResult {
        if (isValid && invitationEntity == null) {
            throw new IllegalArgumentException("Valid result must have an entity");
        }
        if (!isValid && errorCode == null) {
            throw new IllegalArgumentException("Invalid result must have an error code");
        }
    }

    public static ValidationResult valid(InvitationEntity entity) {
        return new ValidationResult(entity, null, true);
    }

    public static ValidationResult invalid(String errorCode) {
        return new ValidationResult(null, errorCode, false);
    }

    public static ValidationResult invalidToken() {
        return invalid("inviteCodeInvalid");
    }

    public static ValidationResult expiredToken() {
        return invalid("inviteCodeInvalid");
    }

    public static ValidationResult usedToken() {
        return invalid("inviteCodeAlreadyUsed");
    }

    public static ValidationResult missingToken() {
        return invalid("inviteCodeMissing");
    }

    public Optional<InvitationEntity> getInvitationEntity() {
        return Optional.ofNullable(invitationEntity);
    }
}