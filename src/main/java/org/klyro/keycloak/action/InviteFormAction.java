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
package org.klyro.keycloak.action;

import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.FormAction;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.klyro.keycloak.service.InvitationService;

/**
 * FormAction that validates invitation tokens during registration.
 * Checks if the invite query parameter contains a valid, unused token.
 */
public class InviteFormAction implements FormAction {
    private KeycloakSession session;

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        // Extract invite token from URL query parameter
        String inviteToken = context.getUriInfo().getQueryParameters().getFirst("inviteCode");

        if (inviteToken != null) {
            // Persist token in Auth Session so it survives validation errors (e.g. bad password)
            context.getAuthenticationSession().setAuthNote("INVITE_TOKEN", inviteToken);
        }
    }

    @Override
    public void validate(ValidationContext context) {
        this.session = context.getSession();
        String token = context.getAuthenticationSession().getAuthNote("INVITE_TOKEN");

        // Extract from URL if not in auth session (first visit)
        if (token == null) {
            token = context.getUriInfo().getQueryParameters().getFirst("inviteCode");
            if (token != null) {
                context.getAuthenticationSession().setAuthNote("INVITE_TOKEN", token);
            }
        }

        String realmId = context.getRealm().getId();

        // If no token in URL, block registration
        if (token == null || token.isEmpty()) {
            // Set the error in the authentication session so the template can display it
            context.getAuthenticationSession().setAuthNote("inviteCodeError", "inviteCodeMissing");
            // Also add as a form message for standard error display
            java.util.List<FormMessage> errors = new java.util.ArrayList<>();
            errors.add(new FormMessage(null, "inviteCodeMissing"));
            context.validationError(context.getHttpRequest().getDecodedFormParameters(), errors);
            return;
        }

        // Use service to validate the invitation token for the current realm
        InvitationService invitationService = new InvitationService(session);
        java.util.Optional<org.klyro.keycloak.entity.InvitationEntity> invitationOpt = invitationService.validateInvite(token, realmId);

        if (invitationOpt.isEmpty()) {
            // Check if expired vs already used vs invalid for specific message
            org.klyro.keycloak.entity.InvitationEntity existing = new InvitationService(session).findByToken(token);

            // Set the error in the authentication session so the template can display it
            String errorCode = (existing != null && existing.isUsed()) ? "inviteCodeAlreadyUsed" : "inviteCodeInvalid";
            context.getAuthenticationSession().setAuthNote("inviteCodeError", errorCode);

            // Use validationError to properly handle the error with form data
            java.util.List<FormMessage> errors = new java.util.ArrayList<>();
            errors.add(new FormMessage(null, errorCode));

            context.validationError(context.getHttpRequest().getDecodedFormParameters(), errors);
            return;
        }

        // Token is valid, allow registration to proceed
        context.getEvent().detail("invite_token", token);
        context.success();
    }

    /**
     * Method for testing purposes to allow dependency injection.
     */
    public void validate(ValidationContext context, InvitationService invitationService) {
        this.session = context.getSession();
        String token = context.getAuthenticationSession().getAuthNote("INVITE_TOKEN");

        // Extract from URL if not in auth session (first visit)
        if (token == null) {
            token = context.getUriInfo().getQueryParameters().getFirst("inviteCode");
            if (token != null) {
                context.getAuthenticationSession().setAuthNote("INVITE_TOKEN", token);
            }
        }

        String realmId = context.getRealm().getId();

        // If no token in URL, block registration
        if (token == null || token.isEmpty()) {
            // Set the error in the authentication session so the template can display it
            context.getAuthenticationSession().setAuthNote("inviteCodeError", "inviteCodeMissing");
            // Also add as a form message for standard error display
            java.util.List<FormMessage> errors = new java.util.ArrayList<>();
            errors.add(new FormMessage(null, "inviteCodeMissing"));
            context.validationError(context.getHttpRequest().getDecodedFormParameters(), errors);
            return;
        }

        // Use service to validate the invitation token for the current realm
        java.util.Optional<org.klyro.keycloak.entity.InvitationEntity> invitationOpt = invitationService.validateInvite(token, realmId);

        if (invitationOpt.isEmpty()) {
            // Check if expired vs already used vs invalid for specific message
            org.klyro.keycloak.entity.InvitationEntity existing = invitationService.findByToken(token);

            // Set the error in the authentication session so the template can display it
            String errorCode = (existing != null && existing.isUsed()) ? "inviteCodeAlreadyUsed" : "inviteCodeInvalid";
            context.getAuthenticationSession().setAuthNote("inviteCodeError", errorCode);

            // Use validationError to properly handle the error with form data
            java.util.List<FormMessage> errors = new java.util.ArrayList<>();
            errors.add(new FormMessage(null, errorCode));

            context.validationError(context.getHttpRequest().getDecodedFormParameters(), errors);
            return;
        }

        // Token is valid, allow registration to proceed
        context.getEvent().detail("invite_token", token);
        context.success();
    }

    @Override
    public void success(FormContext context) {
        // Mark the token as used after successful registration
        String token = context.getAuthenticationSession().getAuthNote("INVITE_TOKEN");

        if (token != null && !token.isEmpty()) {
            InvitationService invitationService = new InvitationService(context.getSession());
            invitationService.markAsUsed(token);
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // No required actions needed
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}