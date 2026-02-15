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

import java.util.List;

/**
 * Response model for paginated invitation list.
 */
public class PaginatedInvitationResponse {
    @JsonProperty("data")
    private List<InvitationListItem> data;

    @JsonProperty("pagination")
    private PaginationInfo pagination;

    public PaginatedInvitationResponse() {
    }

    public PaginatedInvitationResponse(List<InvitationListItem> data, PaginationInfo pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<InvitationListItem> getData() {
        return data;
    }

    public void setData(List<InvitationListItem> data) {
        this.data = data;
    }

    public PaginationInfo getPagination() {
        return pagination;
    }

    public void setPagination(PaginationInfo pagination) {
        this.pagination = pagination;
    }
}