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
 * Request model for pagination parameters.
 */
public class PaginationRequest {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20; // Default page size
    
    private Integer page;

    private Integer size;

    public PaginationRequest() {
    }

    public PaginationRequest(Integer page, Integer size) {
        this.page = page;
        this.size = size;
    }

    @JsonProperty("page")
    public Integer getPage() {
        return page != null ? page : DEFAULT_PAGE;
    }

    @JsonProperty("size")
    public Integer getSize() {
        return size != null ? Math.min(size, 100) : DEFAULT_SIZE; // Cap max page size to 100
    }

    public void setPage(Integer page) {
        this.page = page;
    }


    public void setSize(Integer size) {
        this.size = size;
    }
    
    /**
     * Gets the offset for database queries based on page and size.
     *
     * @return the offset
     */
    public int getOffset() {
        return getPage() * getSize();
    }
}