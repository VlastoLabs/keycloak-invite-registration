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
package org.vlastolabs.keycloak.e2e.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.vlastolabs.keycloak.e2e.JarFileLocator;
import org.vlastolabs.keycloak.e2e.KeycloakContainerConfig;

import java.time.Duration;

public class KeycloakContainerUtil {

    private final KeycloakContainerConfig config;
    private final JarFileLocator jarFileLocator;

    public KeycloakContainerUtil() {
        this(new KeycloakContainerConfig(), new JarFileLocator());
    }

    public KeycloakContainerUtil(KeycloakContainerConfig config, JarFileLocator jarFileLocator) {
        this.config = config;
        this.jarFileLocator = jarFileLocator;
    }

    public GenericContainer<?> createKeycloakContainer(Network network) {
        String jarPath = jarFileLocator.locateJar();

        return new GenericContainer<>(DockerImageName.parse(config.getImageName()))
                .withNetwork(network)
                .withNetworkAliases(config.getNetworkAlias())
                .withExposedPorts(config.getPort(), 9000)
                .withEnv("KC_HEALTH_ENABLED", "true")
                .withEnv("KC_METRICS_ENABLED", "true")
                .withEnv("KEYCLOAK_ADMIN", config.getAdminUsername())
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", config.getAdminPassword())
                // Fix for logging issues - disable JBoss logging
                .withEnv("JAVA_OPTS", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager")
                .withCopyFileToContainer(
                        org.testcontainers.utility.MountableFile.forHostPath(jarPath),
                        config.getProviderPath())
                .withCommand(config.getStartCommand())
                .waitingFor(Wait.forHttp("/health/ready")
                        .forPort(9000)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(5)));
    }
}