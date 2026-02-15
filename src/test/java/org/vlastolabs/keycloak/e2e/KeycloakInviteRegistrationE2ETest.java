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
import org.apache.hc.core5.http.io.entity.StringEntity;
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
import org.vlastolabs.keycloak.model.InviteRequest;
import org.vlastolabs.keycloak.model.PaginatedInvitationResponse;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeycloakInviteRegistrationE2ETest {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakInviteRegistrationE2ETest.class);
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String TEST_REALM = "test-realm";
    private static final String TEST_CLIENT_ID = "test-client";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int CUSTOM_PAGE_SIZE = 3;
    private static final int EXPIRATION_ONE_HOUR = 3600;
    private static final int EXPIRATION_ONE_DAY = 86400;
    private static final int EXPIRATION_TOLERANCE_MS = 10000;

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

        initializeInfrastructure();
        startKeycloak();
        initializeClients();
        setupRealmAndClient();
    }

    @AfterAll
    void tearDown() throws Exception {
        closeQuietly(httpClient);
        stopKeycloak();
        closeQuietly(network);
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGenerateInviteEndpoint() throws Exception {
        executeInviteGenerationTest(null, null);
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGenerateInviteEndpointWithCustomExpiration() throws Exception {
        executeInviteGenerationTest(createInviteRequest(EXPIRATION_ONE_HOUR), EXPIRATION_ONE_HOUR);
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGenerateInviteEndpointWithDefaultExpiration() throws Exception {
        executeInviteGenerationTest(null, EXPIRATION_ONE_DAY);
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGenerateInviteEndpointWithoutAdminAccess() throws Exception {
        var request = buildGenerateInviteRequest("invalid-token");

        try (var response = httpClient.execute(request)) {
            assertUnauthorized(response);
        }
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testCompleteInviteRegistrationFlow() throws Exception {
        var inviteToken = generateInviteToken();
        assertNotNull(inviteToken);
        logger.info("Completed invite registration flow test with token: {}", inviteToken);
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testHealthCheckEndpoint() throws Exception {
        var healthCheck = new HttpGet(healthCheckUrl() + "/health/ready");

        try (var response = httpClient.execute(healthCheck)) {
            assertEquals(200, response.getCode());
        }
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGetAllInvitesEndpoint() throws Exception {
        var accessToken = adminClient.authenticate();
        generateInvite(accessToken, EXPIRATION_ONE_HOUR);

        var request = buildGetAllInvitesRequest(accessToken, null, null);

        try (var response = httpClient.execute(request)) {
            assertEquals(200, response.getCode());

            var paginatedResponse = parsePaginatedResponse(response);
            assertNotNull(paginatedResponse);
            assertNotNull(paginatedResponse.getData());
            assertNotNull(paginatedResponse.getPagination());
            assertTrue(paginatedResponse.getData().size() >= 1);

            var pagination = paginatedResponse.getPagination();
            assertEquals(0, pagination.getPage());
            assertEquals(DEFAULT_PAGE_SIZE, pagination.getSize());
            assertTrue(pagination.getTotalElements() >= 1);
            assertEquals(calculateTotalPages(pagination.getTotalElements(), pagination.getSize()), pagination.getTotalPages());
        }
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGetAllInvitesEndpointWithPagination() throws Exception {
        var accessToken = adminClient.authenticate();

        for (int i = 0; i < 5; i++) {
            generateInvite(accessToken, EXPIRATION_ONE_HOUR);
        }

        var request = buildGetAllInvitesRequest(accessToken, 0, CUSTOM_PAGE_SIZE);

        try (var response = httpClient.execute(request)) {
            assertEquals(200, response.getCode());

            var paginatedResponse = parsePaginatedResponse(response);
            assertNotNull(paginatedResponse);
            assertTrue(paginatedResponse.getData().size() <= CUSTOM_PAGE_SIZE);

            var pagination = paginatedResponse.getPagination();
            assertEquals(0, pagination.getPage());
            assertEquals(CUSTOM_PAGE_SIZE, pagination.getSize());
            assertTrue(pagination.getTotalElements() >= CUSTOM_PAGE_SIZE);
            assertTrue(pagination.getTotalPages() >= 1);
        }
    }

    @Test
    @EnabledIf("isDockerAvailable")
    void testGetAllInvitesEndpointWithoutAdminAccess() throws Exception {
        var request = buildGetAllInvitesRequest("invalid-token", null, null);

        try (var response = httpClient.execute(request)) {
            assertUnauthorized(response);
        }
    }

    private void executeInviteGenerationTest(InviteRequest inviteRequest, Integer expectedExpirationSeconds) throws Exception {
        var accessToken = adminClient.authenticate();
        var request = inviteRequest != null
                ? buildGenerateInviteRequestWithBody(accessToken, inviteRequest)
                : buildGenerateInviteRequest(accessToken);

        try (var response = httpClient.execute(request)) {
            assertEquals(200, response.getCode());

            var inviteResponse = parseInviteResponse(response);
            validateInviteResponse(inviteResponse);

            if (expectedExpirationSeconds != null) {
                validateExpirationTime(inviteResponse, expectedExpirationSeconds);
            }

            logger.info("Generated invite token: {}", inviteResponse.getToken());
        }
    }

    private void validateExpirationTime(InviteGenerationResponse response, int expectedExpirationSeconds) {
        var currentTime = System.currentTimeMillis();
        var expectedExpiration = currentTime + (expectedExpirationSeconds * 1000L);

        assertTrue(
                Math.abs(response.getExpirationTime() - expectedExpiration) < EXPIRATION_TOLERANCE_MS,
                "Expiration time should be approximately %d seconds from now, got: %d, expected around: %d"
                        .formatted(expectedExpirationSeconds, response.getExpirationTime(), expectedExpiration)
        );
    }

    private void initializeInfrastructure() {
        this.network = Network.newNetwork();
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
    }

    private void startKeycloak() {
        this.keycloakContainer = new KeycloakContainerUtil().createKeycloakContainer(network);
        this.keycloakContainer.withLogConsumer(new Slf4jLogConsumer(logger));
        this.keycloakContainer.start();
    }

    private void initializeClients() {
        var keycloakUrl = keycloakUrl();
        this.adminClient = new KeycloakAdminClient(keycloakUrl, ADMIN_USER, ADMIN_PASSWORD, httpClient, objectMapper);
        this.realmManager = new KeycloakRealmManager(keycloakUrl, httpClient);
    }

    private void setupRealmAndClient() throws Exception {
        var accessToken = adminClient.authenticate();
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

    private String keycloakUrl() {
        validateContainer();
        return "http://%s:%d".formatted(keycloakContainer.getHost(), keycloakContainer.getFirstMappedPort());
    }

    private String healthCheckUrl() {
        validateContainer();
        return "http://%s:%d".formatted(keycloakContainer.getHost(), keycloakContainer.getMappedPort(9000));
    }

    private void validateContainer() {
        if (keycloakContainer == null) {
            throw new IllegalStateException("Keycloak container is not available");
        }
    }

    private HttpPost buildGenerateInviteRequest(String accessToken) {
        var request = new HttpPost("%s/admin/realms/%s/invites/generate".formatted(keycloakUrl(), TEST_REALM));
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
        return request;
    }

    private HttpPost buildGenerateInviteRequestWithBody(String accessToken, InviteRequest inviteRequest) throws Exception {
        var request = buildGenerateInviteRequest(accessToken);
        var jsonBody = objectMapper.writeValueAsString(inviteRequest);
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        return request;
    }

    private HttpGet buildGetAllInvitesRequest(String accessToken, Integer page, Integer size) {
        var url = buildUrlWithPagination(page, size);
        var request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Accept", ContentType.APPLICATION_JSON.toString());
        return request;
    }

    private String buildUrlWithPagination(Integer page, Integer size) {
        var baseUrl = "%s/admin/realms/%s/invites".formatted(keycloakUrl(), TEST_REALM);

        if (page == null && size == null) {
            return baseUrl;
        }

        var params = new StringBuilder("?");
        if (page != null) {
            params.append("page=").append(page);
        }
        if (size != null) {
            if (page != null) params.append("&");
            params.append("size=").append(size);
        }

        return baseUrl + params;
    }

    private InviteGenerationResponse parseInviteResponse(ClassicHttpResponse response) throws Exception {
        return objectMapper.readValue(response.getEntity().getContent(), InviteGenerationResponse.class);
    }

    private PaginatedInvitationResponse parsePaginatedResponse(ClassicHttpResponse response) throws Exception {
        return objectMapper.readValue(response.getEntity().getContent(), PaginatedInvitationResponse.class);
    }

    private void validateInviteResponse(InviteGenerationResponse response) {
        assertNotNull(response.getToken());
        assertEquals(TEST_REALM, response.getRealm());
        assertEquals("Invitation token generated successfully", response.getMessage());
        assertTrue(response.getExpirationTime() > 0);
        assertFalse(response.isUsed());
    }

    private String generateInviteToken() throws Exception {
        var accessToken = adminClient.authenticate();
        var request = buildGenerateInviteRequest(accessToken);

        try (var response = httpClient.execute(request)) {
            assertEquals(200, response.getCode());
            return parseInviteResponse(response).getToken();
        }
    }

    private void generateInvite(String accessToken, int expirationSeconds) throws Exception {
        var request = buildGenerateInviteRequestWithBody(accessToken, createInviteRequest(expirationSeconds));
        try (var response = httpClient.execute(request)) {
            assertEquals(200, response.getCode());
        }
    }

    private InviteRequest createInviteRequest(int expirationSeconds) {
        var request = new InviteRequest();
        request.setExpirationTime(expirationSeconds);
        return request;
    }

    private void assertUnauthorized(ClassicHttpResponse response) {
        assertTrue(response.getCode() == 401 || response.getCode() == 403);
    }

    private int calculateTotalPages(long totalElements, int pageSize) {
        return (int) Math.ceil((double) totalElements / pageSize);
    }

    private void stopKeycloak() {
        if (keycloakContainer != null && keycloakContainer.isRunning()) {
            keycloakContainer.stop();
        }
    }

    private void closeQuietly(AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }
}