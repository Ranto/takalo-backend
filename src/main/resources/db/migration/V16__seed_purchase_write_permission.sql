-- Permission manquante référencée par PurchaseController (réassignation de budget,
-- single et bulk) — sans cette ligne, les endpoints répondent 403 "Permission insuffisante".
INSERT INTO permissions (id, name, description)
VALUES ('00000000-0000-0000-0000-000000000114', 'purchase:write', 'Modifier un achat (réassigner son budget)');

-- ADMIN et USER reçoivent la permission : un utilisateur peut réassigner ses propres achats,
-- les contrôles d'auteur et d'éditeur de budget sont effectués au niveau du service.
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000114'),
    ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000114');
