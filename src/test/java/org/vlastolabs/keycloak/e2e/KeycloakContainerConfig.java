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
package org.vlastolabs.keycloak.e2e;

/**
 * Configuration for Keycloak container setup.
 * Follows Open/Closed Principle - open for extension via builder, closed for modification.
 */
public class KeycloakContainerConfig {
    
    private final String imageName;
    private final int port;
    private final String networkAlias;
    private final String adminUsername;
    private final String adminPassword;
    private final String providerPath;
    private final String startCommand;
    
    /**
     * Creates a configuration with default values.
     */
    public KeycloakContainerConfig() {
        this("quay.io/keycloak/keycloak:26.0.0", 8080, "keycloak", "admin", "admin",
             "/opt/keycloak/providers/keycloak-invite-registration.jar", "start-dev");
    }
    
    /**
     * Creates a configuration with custom values.
     */
    public KeycloakContainerConfig(String imageName, int port, String networkAlias, 
                                   String adminUsername, String adminPassword,
                                   String providerPath, String startCommand) {
        this.imageName = imageName;
        this.port = port;
        this.networkAlias = networkAlias;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.providerPath = providerPath;
        this.startCommand = startCommand;
    }
    
    public String getImageName() {
        return imageName;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getNetworkAlias() {
        return networkAlias;
    }
    
    public String getAdminUsername() {
        return adminUsername;
    }
    
    public String getAdminPassword() {
        return adminPassword;
    }
    
    public String getProviderPath() {
        return providerPath;
    }
    
    public String getStartCommand() {
        return startCommand;
    }
}
