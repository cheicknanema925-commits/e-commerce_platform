-- Identifiant de l'utilisateur chez Keycloak (claim « sub », un UUID) : relie un compte Keycloak à un compte local,
-- nécessaire pour rattacher ses commandes, son panier et ses avis
ALTER TABLE users ADD COLUMN external_id VARCHAR(64) UNIQUE;
