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

import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class InviteFormActionFactory implements FormActionFactory {
    public static final String PROVIDER_ID = "registration-invite-action";

    @Override
    public String getDisplayType() { return "Registration Invite Gate"; }

    @Override
    public String getReferenceCategory() {
        return "";
    }

    @Override
    public String getHelpText() { return "Requires a specific code to register."; }

    @Override
    public String getId() { return PROVIDER_ID; }

    @Override
    public FormAction create(KeycloakSession session) { return new InviteFormAction(); }

    // Required by Factory Interface
    @Override public void init(Config.Scope config) {}
    @Override public void postInit(KeycloakSessionFactory factory) {}
    @Override public void close() {}

    @Override public boolean isConfigurable() { return true; }
    @Override public boolean isUserSetupAllowed() { return false; }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] { AuthenticationExecutionModel.Requirement.REQUIRED };
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName("invite_code_secret");
        property.setLabel("Valid Invite Code");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Users must enter this code to sign up.");
        return List.of(property);
    }
}