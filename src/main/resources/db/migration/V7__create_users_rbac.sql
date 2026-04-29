CREATE TABLE users
(
    id           UUID                        NOT NULL,
    external_id  VARCHAR(255)                NOT NULL,
    email        VARCHAR(255)                NOT NULL,
    display_name VARCHAR(255),
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_external_id UNIQUE (external_id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE roles
(
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE permissions
(
    id          UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_name UNIQUE (name)
);

CREATE TABLE user_roles
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_on_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_on_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE role_permissions
(
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_on_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_on_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role ON user_roles (role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);

-- Seed roles
INSERT INTO roles (id, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Administrateur — accès complet'),
    ('00000000-0000-0000-0000-000000000002', 'USER',  'Utilisateur standard');

-- Seed permissions
INSERT INTO permissions (id, name, description) VALUES
    ('00000000-0000-0000-0000-000000000101', 'category:manage',     'Gérer les catégories'),
    ('00000000-0000-0000-0000-000000000102', 'category:read',       'Lire les catégories'),
    ('00000000-0000-0000-0000-000000000103', 'product:write',       'Créer/modifier/supprimer des produits'),
    ('00000000-0000-0000-0000-000000000104', 'product:read',        'Lire les produits'),
    ('00000000-0000-0000-0000-000000000105', 'purchase:create',     'Créer un achat'),
    ('00000000-0000-0000-0000-000000000106', 'purchase:read:own',   'Lire ses propres achats'),
    ('00000000-0000-0000-0000-000000000107', 'purchase:read:any',   'Lire tous les achats'),
    ('00000000-0000-0000-0000-000000000108', 'purchase:import',     'Importer des achats'),
    ('00000000-0000-0000-0000-000000000109', 'user:manage',         'Gérer les utilisateurs et rôles');

-- ADMIN: toutes les permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permissions;

-- USER: lectures + créer/lire ses achats
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000102'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000104'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000105'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000106');
