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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.vlastolabs.keycloak.model.InviteGenerationResponse;
import org.vlastolabs.keycloak.model.InviteRequest;
import org.vlastolabs.keycloak.model.PaginatedInvitationResponse;
import org.vlastolabs.keycloak.service.InvitationService;

import java.util.Optional;

public class InvitationResource {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final KeycloakSession session;
    private final RealmModel realmModel;
    private final AdminPermissionEvaluator adminPermissionEvaluator;
    private final InvitationService invitationService;

    public InvitationResource(KeycloakSession session, RealmModel realmModel, AdminPermissionEvaluator adminPermissionEvaluator) {
        this(session, realmModel, adminPermissionEvaluator, new InvitationService(session));
    }

    public InvitationResource(KeycloakSession session, RealmModel realmModel, AdminPermissionEvaluator adminPermissionEvaluator, InvitationService invitationService) {
        this.session = session;
        this.realmModel = realmModel;
        this.adminPermissionEvaluator = adminPermissionEvaluator;
        this.invitationService = invitationService;
    }

    @POST
    @Path("generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateInvite(InviteRequest request) {
        if (!isAdmin()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            var response = Optional.ofNullable(request)
                    .map(InviteRequest::getExpirationTime)
                    .filter(expTime -> expTime > 0)
                    .map(expTime -> invitationService.generateInvite(realmModel, expTime))
                    .orElseGet(() -> invitationService.generateInvite(realmModel));

            return Response.ok(response).build();
        } catch (Exception e) {
            return errorResponse("Failed to generate invitation token: " + e.getMessage());
        }
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllInvites(@QueryParam("page") Integer page, @QueryParam("size") Integer size) {
        if (!isAdmin()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            var pageNum = normalizePageNumber(page);
            var pageSize = normalizePageSize(size);
            var response = invitationService.getAllInvitationsPaginated(pageNum, pageSize);

            return Response.ok(response).build();
        } catch (Exception e) {
            return errorResponse("Failed to retrieve invitations: " + e.getMessage());
        }
    }

    private boolean isAdmin() {
        return adminPermissionEvaluator.adminAuth().hasRealmRole(AdminRoles.ADMIN);
    }

    private int normalizePageNumber(Integer page) {
        return page != null ? Math.max(0, page) : DEFAULT_PAGE;
    }

    private int normalizePageSize(Integer size) {
        return size != null ? Math.max(1, Math.min(size, MAX_PAGE_SIZE)) : DEFAULT_PAGE_SIZE;
    }

    private Response errorResponse(String message) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(message))
                .build();
    }

    public record ErrorResponse(String error) {
    }
}

