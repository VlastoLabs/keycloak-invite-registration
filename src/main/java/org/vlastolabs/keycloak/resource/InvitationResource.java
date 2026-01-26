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
package org.vlastolabs.keycloak.resource;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.vlastolabs.keycloak.model.InviteGenerationResponse;
import org.vlastolabs.keycloak.service.InvitationService;

/**
 * REST endpoint for generating invitation tokens.
 * Accessible only to users with admin realm role.
 */
public class InvitationResource {
    private final KeycloakSession session;
    private final RealmModel realmModel;
    private final AdminPermissionEvaluator adminPermissionEvaluator;
    private final InvitationService invitationService;

    public InvitationResource(KeycloakSession session, RealmModel realmModel, AdminPermissionEvaluator adminPermissionEvaluator) {
        this.session = session;
        this.realmModel = realmModel;
        this.adminPermissionEvaluator = adminPermissionEvaluator;
        this.invitationService = new InvitationService(session);
    }

    /**
     * Constructor for testing purposes to inject service dependency.
     */
    public InvitationResource(KeycloakSession session, RealmModel realmModel, AdminPermissionEvaluator adminPermissionEvaluator, InvitationService invitationService) {
        this.session = session;
        this.realmModel = realmModel;
        this.adminPermissionEvaluator = adminPermissionEvaluator;
        this.invitationService = invitationService;
    }

    /**
     * Generate a new invitation token.
     * Only accessible to users with admin realm role.
     *
     * @return JSON response with the generated token
     */
    @POST
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateInvite() {

        if (!adminPermissionEvaluator.adminAuth().hasRealmRole(AdminRoles.ADMIN)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            InviteGenerationResponse response = invitationService.generateInvite(realmModel);
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate invitation token: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Error response model for API errors.
     */
    public static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}

