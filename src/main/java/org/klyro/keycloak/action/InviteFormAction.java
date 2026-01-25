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
package org.klyro.keycloak.action;

import org.jboss.logging.Logger;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.klyro.keycloak.model.ValidationResult;
import org.klyro.keycloak.service.InvitationService;

import java.util.List;
import java.util.Optional;

/**
 * FormAction that validates invitation tokens during registration.
 * Enforces invite-only registration by requiring a valid, unused token.
 */
public class InviteFormAction implements FormAction {
    private static final Logger log = Logger.getLogger(InviteFormAction.class);

    private static final String INVITE_PARAM = "inviteCode";
    private static final String AUTH_NOTE_KEY = "INVITE_TOKEN";

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        extractAndStoreToken(context);
    }

    @Override
    public void validate(ValidationContext context) {
        validate(context, createInvitationService(context.getSession()));
    }

    void validate(ValidationContext context, InvitationService invitationService) {
        var tokenOpt = getOrExtractToken(context);

        if (tokenOpt.isEmpty()) {
            handleMissingToken(context);
            return;
        }

        String token = tokenOpt.get();
        String realmId = context.getRealm().getId();
        ValidationResult result = invitationService.validateInviteDetailed(token, realmId);

        handleValidationResult(context, result);
    }

    @Override
    public void success(FormContext context) {
        getStoredToken(context).ifPresent(token -> markTokenAsUsed(context, token));
    }

    private void extractAndStoreToken(FormContext context) {
        getQueryParameter(context, INVITE_PARAM)
                .ifPresent(token -> {
                    storeToken(context, token);
                    logDebug("Stored invite token in auth session", context.getRealm());
                });
    }

    private Optional<String> getOrExtractToken(ValidationContext context) {
        return getStoredToken(context)
                .or(() -> getQueryParameter(context, INVITE_PARAM)
                        .map(token -> {
                            storeToken(context, token);
                            return token;
                        }));
    }

    private Optional<String> getStoredToken(FormContext context) {
        return Optional.ofNullable(context.getAuthenticationSession().getAuthNote(AUTH_NOTE_KEY))
                .filter(token -> !token.isBlank());
    }

    private Optional<String> getQueryParameter(FormContext context, String paramName) {
        return Optional.ofNullable(context.getUriInfo().getQueryParameters().getFirst(paramName))
                .filter(value -> !value.isBlank());
    }

    private void storeToken(FormContext context, String token) {
        context.getAuthenticationSession().setAuthNote(AUTH_NOTE_KEY, token);
    }

    private void handleMissingToken(ValidationContext context) {
        logWarn("Registration blocked - no invite token", context.getRealm());
        setValidationError(context, "inviteCodeMissing");
    }

    private void handleValidationResult(ValidationContext context, ValidationResult result) {
        if (result.isValid()) {
            handleValidToken(context);
        } else {
            handleInvalidToken(context, result.errorCode());
        }
    }

    private void handleValidToken(ValidationContext context) {
        logDebug("Valid invite token accepted", context.getRealm());
        context.success();
    }

    private void handleInvalidToken(ValidationContext context, String errorCode) {
        logWarn("Invalid invite token (error: %s)".formatted(errorCode), context.getRealm());
        setValidationError(context, errorCode);
    }

    private void setValidationError(ValidationContext context, String errorCode) {
        var errors = List.of(new FormMessage(null, errorCode));
        context.validationError(context.getHttpRequest().getDecodedFormParameters(), errors);
    }

    private void markTokenAsUsed(FormContext context, String token) {
        try {
            var invitationService = createInvitationService(context.getSession());
            invitationService.markAsUsed(token);
            logDebug("Invitation token marked as used", context.getRealm());
        } catch (Exception e) {
            log.errorf(e, "Failed to mark invitation as used - registration still succeeded");
        }
    }

    private InvitationService createInvitationService(KeycloakSession session) {
        return new InvitationService(session);
    }

    private void logDebug(String message, RealmModel realm) {
        log.debugf("%s for realm: %s", message, realm.getName());
    }

    private void logWarn(String message, RealmModel realm) {
        log.warnf("%s for realm: %s", message, realm.getName());
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}