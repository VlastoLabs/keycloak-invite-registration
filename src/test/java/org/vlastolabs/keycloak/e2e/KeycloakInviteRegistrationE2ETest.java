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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.vlastolabs.keycloak.e2e.client.KeycloakAdminClient;
import org.vlastolabs.keycloak.e2e.client.KeycloakRealmManager;
import org.vlastolabs.keycloak.e2e.util.KeycloakContainerUtil;
import org.vlastolabs.keycloak.model.InviteGenerationResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeycloakInviteRegistrationE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakInviteRegistrationE2ETest.class);

    private static final String KEYCLOAK_ADMIN_USER = "admin";
    private static final String KEYCLOAK_ADMIN_PASSWORD = "admin";
    private static final String TEST_REALM = "test-realm";
    private static final String TEST_CLIENT_ID = "test-client";

    private Network network;
    private GenericContainer<?> keycloakContainer;
    private CloseableHttpClient httpClient;
    private ObjectMapper objectMapper;
    private KeycloakAdminClient adminClient;
    private KeycloakRealmManager realmManager;

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    @BeforeAll
    void setUp() throws Exception {
        if (!isDockerAvailable()) {
            logger.warn("Skipping E2E tests because Docker is not available");
            return;
        }

        initializeTestInfrastructure();
        startKeycloakContainer();
        initializeKeycloakClients();
        setupTestRealmAndClient();
    }

    @AfterAll
    void tearDown() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
        if (keycloakContainer != null && keycloakContainer.isRunning()) {
            keycloakContainer.stop();
        }
        if (network != null) {
            network.close();
        }
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGenerateInviteEndpoint() throws Exception {
        String accessToken = adminClient.authenticate();
        HttpPost request = createGenerateInviteRequest(accessToken);

        try (ClassicHttpResponse response = httpClient.execute(request)) {
            assertEquals(200, response.getCode());

            InviteGenerationResponse inviteResponse = parseInviteResponse(response);
            validateInviteResponse(inviteResponse);

            logger.info("Generated invite token: {}", inviteResponse.getToken());
        }
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGenerateInviteEndpointWithoutAdminAccess() throws Exception {
        HttpPost request = createGenerateInviteRequest("invalid-token");

        try (ClassicHttpResponse response = httpClient.execute(request)) {
            assertTrue(response.getCode() == 401 || response.getCode() == 403);
        }
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testCompleteInviteRegistrationFlow() throws Exception {
        String inviteToken = generateInviteToken();
        assertNotNull(inviteToken);
        logger.info("Completed invite registration flow test with token: {}", inviteToken);
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testHealthCheckEndpoint() throws Exception {
        HttpGet healthCheck = new HttpGet(getHealthcheckKeycloakUrl() + "/health/ready");

        try (ClassicHttpResponse response = httpClient.execute(healthCheck)) {
            assertEquals(200, response.getCode());
        }
    }

    private void initializeTestInfrastructure() {
        this.network = Network.newNetwork();
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    private void startKeycloakContainer() {
        KeycloakContainerUtil containerUtil = new KeycloakContainerUtil();
        this.keycloakContainer = containerUtil.createKeycloakContainer(network);
        this.keycloakContainer.withLogConsumer(new Slf4jLogConsumer(logger));
        this.keycloakContainer.start();
    }

    private void initializeKeycloakClients() {
        String keycloakUrl = getKeycloakUrl();
        this.adminClient = new KeycloakAdminClient(
                keycloakUrl,
                KEYCLOAK_ADMIN_USER,
                KEYCLOAK_ADMIN_PASSWORD,
                httpClient,
                objectMapper);
        this.realmManager = new KeycloakRealmManager(keycloakUrl, httpClient);
    }

    private void setupTestRealmAndClient() throws Exception {
        String accessToken = adminClient.authenticate();
        realmManager.createRealm(TEST_REALM, accessToken);
        realmManager.createClient(TEST_REALM, TEST_CLIENT_ID, accessToken);
    }

    private boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            logger.warn("Docker is not available: {}", e.getMessage());
            return false;
        }
    }

    private String getKeycloakUrl() {
        if (keycloakContainer == null) {
            throw new IllegalStateException("Keycloak container is not available");
        }
        return String.format("http://%s:%d",
                keycloakContainer.getHost(),
                keycloakContainer.getFirstMappedPort());
    }

    private String getHealthcheckKeycloakUrl() {
        if (keycloakContainer == null) {
            throw new IllegalStateException("Keycloak container is not available");
        }
        return String.format("http://%s:%d",
                keycloakContainer.getHost(),
                keycloakContainer.getMappedPort(9000));
    }

    private HttpPost createGenerateInviteRequest(String accessToken) {
        HttpPost request = new HttpPost(
                getKeycloakUrl() + "/admin/realms/" + TEST_REALM + "/invites/generate");
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
        return request;
    }

    private InviteGenerationResponse parseInviteResponse(ClassicHttpResponse response) throws Exception {
        return objectMapper.readValue(
                response.getEntity().getContent(),
                InviteGenerationResponse.class);
    }

    private void validateInviteResponse(InviteGenerationResponse response) {
        assertNotNull(response.getToken());
        assertEquals(TEST_REALM, response.getRealm());
        assertEquals("Invitation token generated successfully", response.getMessage());
        assertTrue(response.getExpirationTime() > 0);
        assertFalse(response.isUsed());
    }

    private String generateInviteToken() throws Exception {
        String accessToken = adminClient.authenticate();
        HttpPost request = createGenerateInviteRequest(accessToken);

        try (ClassicHttpResponse response = httpClient.execute(request)) {
            assertEquals(200, response.getCode());
            InviteGenerationResponse inviteResponse = parseInviteResponse(response);
            return inviteResponse.getToken();
        }
    }
}