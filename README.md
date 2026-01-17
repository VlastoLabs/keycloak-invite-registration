# Keycloak Invite Registration Extension

A Keycloak SPI extension that enables invitation-only user registration with time-limited tokens.

## Project Overview

- Generate time-limited invitation tokens via admin REST API
- Restrict user registration to invited users only
- Automatic token expiration and usage tracking
- Integration with Keycloak's form action workflow

### Use Cases
- Invite-only registration for organizations
- Controlled user onboarding
- Temporary access grants
- Beta testing programs

## Installation

### Prerequisites
- Keycloak 26.0.0 or higher
- Java 21 or higher

### Build the Extension
```bash
mvn clean install
```

### Deployment
1. Copy the generated JAR file from `target/keycloak-invite-registration-1.0-SNAPSHOT.jar` to your Keycloak server's `providers` directory
2. Restart Keycloak server

### Required SPI Registration
The extension automatically registers itself through Java SPI mechanism. No additional configuration is required.

## Configuration

### Form Action Setup
1. Navigate to Authentication → Registration → Bindings
2. Add the "Registration Invite Gate" form action
3. Configure as REQUIRED execution


### Admin Permissions
The REST endpoint requires users to have the `admin` realm role to generate invitation tokens.

## REST API

### Generate Invitation Token
- **Endpoint**: `POST /admin/realms/{realm}/invites/generate`
- **Authentication**: Requires admin realm role
- **Request**: No request body required
- **Response**: JSON object with invitation details

#### Example Request
```bash
curl -X POST \
  http://localhost:8080/admin/realms/myrealm/invites/generate \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json'
```

#### Example Response
```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "realm": "myrealm",
  "message": "Invitation token generated successfully",
  "expirationTime": 1703123456789,
  "used": false
}
```

## Development

### Running Tests
```bash
mvn test
```

### Building the Project
```bash
mvn clean package
```

### Running Keycloak with Extension (Development)
```bash
# Build the project
mvn clean package

# Copy JAR to Keycloak providers directory
cp target/keycloak-invite-registration-1.0-SNAPSHOT.jar /path/to/keycloak/providers/

# Start Keycloak
/path/to/keycloak/bin/kc.sh start
```
## Configuration Options
Currently, the default expiration time is 24 hours and cannot be configured. Future versions may support configuration via Keycloak's configuration system.


## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE.txt) file for details.