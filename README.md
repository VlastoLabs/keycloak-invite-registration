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
- **Request Body**: Optional JSON object with custom expiration time
  ```json
  {
    "expirationTime": 3600
  }
  ```
  Where `expirationTime` is the number of seconds until expiration (defaults to 24 hours if not provided)
- **Response**: JSON object with invitation details

#### Example Request
```bash
curl -X POST \
  http://localhost:8080/admin/realms/myrealm/invites/generate \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{"expirationTime": 3600}'
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

### Get All Invitation Tokens (with pagination)
- **Endpoint**: `GET /admin/realms/{realm}/invites`
- **Authentication**: Requires admin realm role
- **Query Parameters**:
  - `page` (optional): Page number (0-indexed, defaults to 0)
  - `size` (optional): Page size (defaults to 20, max 100)
- **Response**: JSON object with paginated list of invitation tokens

#### Example Request
```bash
curl -X GET \
  http://localhost:8080/admin/realms/myrealm/invites?page=0&size=10 \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Accept: application/json'
```

#### Example Response
```json
{
  "data": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "token": "xyz789...",
      "used": false,
      "realm": "myrealm",
      "createdOn": 1703123456789,
      "expiresOn": 1703209856789
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  }
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