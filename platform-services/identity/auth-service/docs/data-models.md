# Auth App Data Models

This document provides a comprehensive overview of the data models used in the Auth App, including their structure,
relationships, and purpose.

## Table of Contents

- [Overview](#overview)
- [Core Entities](#core-entities)
    - [User](#user)
    - [UserAddress](#useraddress)
    - [Roles](#roles)
    - [Permissions](#permissions)
- [Location Entities](#location-entities)
    - [Country](#country)
    - [States](#states)
    - [City](#city)
- [Entity Relationships](#entity-relationships)
- [Database Schema](#database-schema)
- [Auditing](#auditing)

## Overview

The Auth App uses a relational database model with several interconnected entities. The data model is designed to
support:

1. User authentication and management
2. Role-based access control (RBAC)
3. User address and location management
4. Comprehensive auditing

All entities extend an `Auditable` base class that provides common audit fields, and most entities are annotated with
Hibernate's `@Audited` for change tracking.

## Core Entities

### User

The `User` entity is the central entity in the Auth App, representing a user account.

#### Attributes

| Attribute             | Type    | Description                   | Constraints                                         |
|-----------------------|---------|-------------------------------|-----------------------------------------------------|
| userId                | Long    | Primary key                   | Not null, auto-generated                            |
| firstName             | String  | User's first name             | Not null, max 50 chars                              |
| middleName            | String  | User's middle name            | Max 50 chars                                        |
| lastName              | String  | User's last name              | Not null, max 50 chars                              |
| userName              | String  | Unique username               | Not null, 3-16 chars, unique                        |
| password              | String  | Hashed password               | Not null, min 8 chars, max 255 chars                |
| email                 | String  | Primary email                 | Not null, valid email format, max 100 chars, unique |
| alternateEmail        | String  | Secondary email               | Valid email format, max 100 chars                   |
| emailVerified         | Boolean | Email verification status     | Not null                                            |
| phoneCode             | String  | Country phone code            | Not null, max 5 chars                               |
| contactNumber         | String  | Primary phone number          | Not null, 10-15 chars, valid format, unique         |
| alternateContact      | String  | Secondary phone number        | 10-15 chars, valid format                           |
| phoneNumberVerified   | Boolean | Phone verification status     | Not null                                            |
| login                 | Instant | Last login timestamp          |                                                     |
| logout                | Instant | Last logout timestamp         |                                                     |
| accountNonExpired     | Boolean | Account expiration status     | Not null                                            |
| accountNonLocked      | Boolean | Account lock status           | Not null                                            |
| accountLocked         | Boolean | Account lock status           | Not null                                            |
| credentialsNonExpired | Boolean | Credentials expiration status | Not null                                            |
| activeInd             | Boolean | Active status                 | Not null                                            |

#### Indexes

- `idx_user_username`: Index on `user_name` column
- `idx_user_email`: Index on `email` column
- `idx_user_contact_number`: Index on `contact_number` column
- `idx_user_active_ind`: Index on `active_ind` column

#### Unique Constraints

- `uk_user_username`: Unique constraint on `user_name` column
- `uk_user_email`: Unique constraint on `email` column
- `uk_user_contact_number`: Unique constraint on `contact_number` column

### UserAddress

The `UserAddress` entity represents a physical address associated with a user.

#### Attributes

| Attribute        | Type          | Description            | Constraints                |
|------------------|---------------|------------------------|----------------------------|
| userAddressId    | Long          | Primary key            | Not null, auto-generated   |
| addressUuid      | String        | Unique identifier      | Not null, unique, 36 chars |
| label            | String        | Address label          | Max 50 chars               |
| addressLine1     | String        | Primary address line   | Not null, max 255 chars    |
| addressLine2     | String        | Secondary address line | Max 255 chars              |
| landmark         | String        | Nearby landmark        | Max 255 chars              |
| pincode          | String        | Postal code            | Not null, exactly 6 digits |
| addressType      | AddressType   | Type of address (enum) | Not null                   |
| isDefault        | Boolean       | Default address flag   | Not null                   |
| status           | AddressStatus | Address status (enum)  | Not null                   |
| isVerified       | Boolean       | Verification status    | Not null                   |
| isPoBox          | Boolean       | PO Box indicator       | Not null                   |
| timezone         | String        | Timezone               | Max 50 chars               |
| languageCode     | String        | Language code          | Max 5 chars                |
| formattedAddress | String        | Full formatted address | Text                       |
| latitude         | BigDecimal    | Latitude coordinate    | Precision 10, scale 7      |
| longitude        | BigDecimal    | Longitude coordinate   | Precision 10, scale 7      |

#### Indexes

- `idx_user_address_user_id`: Index on `user_id` column
- `idx_user_address_country_id`: Index on `country_id` column
- `idx_user_address_state_id`: Index on `state_id` column
- `idx_user_address_city_id`: Index on `city_id` column
- `idx_user_address_pincode`: Index on `pincode` column

#### Unique Constraints

- `uk_user_address_uuid`: Unique constraint on `address_uuid` column

### Roles

The `Roles` entity represents a role that can be assigned to users, containing a set of permissions.

#### Attributes

| Attribute   | Type    | Description      | Constraints              |
|-------------|---------|------------------|--------------------------|
| roleId      | Long    | Primary key      | Not null, auto-generated |
| roleName    | String  | Name of the role | Not null, max 20 chars   |
| description | String  | Role description | Max 200 chars            |
| activeInd   | Boolean | Active status    | Not null                 |

#### Indexes

- `idx_role_name`: Index on `role_name` column

#### Unique Constraints

- `uk_role_name`: Unique constraint on `role_name` column

### Permissions

The `Permissions` entity represents a specific permission that can be assigned to roles.

#### Attributes

| Attribute      | Type    | Description            | Constraints              |
|----------------|---------|------------------------|--------------------------|
| permissionId   | Long    | Primary key            | Not null, auto-generated |
| permissionName | String  | Name of the permission | Not null, max 50 chars   |
| activeInd      | Boolean | Active status          | Not null                 |

#### Indexes

- `idx_permission_name`: Index on `permission_name` column

#### Unique Constraints

- `uk_permission_name`: Unique constraint on `permission_name` column

## Location Entities

### Country

The `Country` entity represents a country in the system.

#### Attributes

| Attribute | Type    | Description   | Constraints |
|-----------|---------|---------------|-------------|
| name      | String  | Country name  | Not null    |
| code      | String  | Country code  | Not null    |
| activeInd | Boolean | Active status | Not null    |

### States

The `States` entity represents a state or province within a country.

#### Attributes

| Attribute | Type    | Description          | Constraints |
|-----------|---------|----------------------|-------------|
| name      | String  | State name           | Not null    |
| code      | String  | State code           | Not null    |
| activeInd | Boolean | Active status        | Not null    |
| countryId | Long    | Reference to country | Not null    |

### City

The `City` entity represents a city within a state.

#### Attributes

| Attribute | Type    | Description        | Constraints |
|-----------|---------|--------------------|-------------|
| name      | String  | City name          | Not null    |
| activeInd | Boolean | Active status      | Not null    |
| stateName | String  | Reference to state | Not null    |

## Entity Relationships

The Auth App uses several relationships between entities:

### User Relationships

- **User to Roles**: Many-to-Many
    - Implemented via a join table `user_role`
    - A user can have multiple roles
    - A role can be assigned to multiple users

- **User to UserAddress**: One-to-Many
    - A user can have multiple addresses
    - Each address belongs to exactly one user

### Role Relationships

- **Roles to Permissions**: Many-to-Many
    - Implemented via a join table `role_permission`
    - A role can have multiple permissions
    - A permission can be part of multiple roles

- **Roles to User**: Many-to-Many (inverse of User to Roles)

### UserAddress Relationships

- **UserAddress to User**: Many-to-One
    - Each address belongs to exactly one user
    - A user can have multiple addresses

- **UserAddress to Country**: Many-to-One
    - Each address belongs to exactly one country
    - A country can have multiple addresses

- **UserAddress to States**: Many-to-One
    - Each address belongs to exactly one state
    - A state can have multiple addresses

- **UserAddress to City**: Many-to-One
    - Each address belongs to exactly one city
    - A city can have multiple addresses

### Location Relationships

- **States to Country**: Many-to-One
    - Each state belongs to exactly one country
    - A country can have multiple states

- **City to States**: Many-to-One
    - Each city belongs to exactly one state
    - A state can have multiple cities

## Database Schema

The Auth App uses the following database schemas:

- `user_auth`: Contains user authentication and authorization tables
    - `users`: User accounts
    - `user_address`: User addresses
    - `role`: User roles
    - `permission`: Role permissions
    - `user_role`: Join table for users and roles
    - `role_permission`: Join table for roles and permissions

- `auth`: Contains database sequences
    - `user_sequence`: Sequence for user IDs
    - `user_address_sequence`: Sequence for user address IDs

## Auditing

All entities in the Auth App extend an `Auditable<String>` base class that provides the following audit fields:

- `createdBy`: The user who created the record
- `createdDate`: The timestamp when the record was created
- `lastModifiedBy`: The user who last modified the record
- `lastModifiedDate`: The timestamp when the record was last modified

Additionally, most entities are annotated with Hibernate's `@Audited` annotation, which enables change tracking through
Hibernate Envers. This creates audit tables that track all changes to the entities, allowing for a complete history of
changes to be maintained.