# ADR 002: Role-Based Access Control (RBAC)

## Status

Accepted

## Context

The Auth App needs a robust authorization mechanism to control access to resources and operations. We need an approach
that:

1. Provides fine-grained access control
2. Scales with growing number of users and resources
3. Is flexible enough to accommodate different types of applications
4. Simplifies permission management
5. Integrates well with our authentication mechanism
6. Supports hierarchical access control

## Decision

We will implement a three-tier Role-Based Access Control (RBAC) model with the following components:

1. **Users**: Individual accounts that authenticate to the system
2. **Roles**: Named collections of permissions that can be assigned to users
3. **Permissions**: Fine-grained access rights that define what operations can be performed

### Implementation Details

1. **Data Model**:
    - `User` entity with many-to-many relationship to `Roles`
    - `Roles` entity with many-to-many relationship to `Permissions`
    - `Permissions` entity representing individual access rights

2. **Permission Granularity**:
    - CRUD operations (CREATE, READ, UPDATE, DELETE)
    - Additional operations like SEND_EMAIL, APPROVE, etc.
    - Resource-specific permissions

3. **Role Management**:
    - Predefined system roles (ADMIN, USER, GUEST)
    - Custom roles for specific business needs
    - Role hierarchies (e.g., ADMIN inherits all USER permissions)

4. **Integration with Spring Security**:
    - Custom `UserDetailsService` to load user roles and permissions
    - Method-level security with `@PreAuthorize` annotations
    - Custom permission evaluators for complex authorization rules
    - JWT claims to include roles and permissions

5. **Authorization Enforcement**:
    - At API gateway level for coarse-grained control
    - At service level for fine-grained control
    - At method level for business logic control

## Consequences

### Advantages

1. **Simplified Administration**: Permissions are assigned to roles, not directly to users, reducing management
   complexity.
2. **Scalability**: The model scales well with increasing numbers of users.
3. **Flexibility**: Custom roles can be created for specific business needs.
4. **Auditability**: Role and permission changes can be tracked and audited.
5. **Principle of Least Privilege**: Users can be given only the permissions they need.

### Disadvantages

1. **Complexity**: Three-tier RBAC is more complex than simpler models.
2. **Performance**: Authorization checks add overhead to each request.
3. **Role Explosion**: Without careful management, the number of roles can grow excessively.
4. **Initial Setup**: Requires significant upfront design of roles and permissions.

### Mitigations

1. **Complexity**: Provide administrative interfaces for role and permission management.
2. **Performance**: Cache user permissions and optimize authorization checks.
3. **Role Explosion**: Implement role hierarchies and regular role audits.
4. **Initial Setup**: Start with a minimal set of roles and permissions, then expand as needed.

## Alternatives Considered

1. **Attribute-Based Access Control (ABAC)**:
    - Pros: More flexible, can handle complex rules based on user attributes, resource attributes, and context
    - Cons: More complex to implement and manage, potentially higher performance impact

2. **Simple Role-Based Model (without Permissions)**:
    - Pros: Simpler to implement, lower overhead
    - Cons: Less flexible, coarser granularity

3. **Access Control Lists (ACLs)**:
    - Pros: Very fine-grained control at the resource level
    - Cons: Difficult to manage at scale, complex implementation

## References

- [NIST RBAC Standard](https://csrc.nist.gov/projects/role-based-access-control)
- [Spring Security Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)
- [OWASP Access Control Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Access_Control_Cheat_Sheet.html)