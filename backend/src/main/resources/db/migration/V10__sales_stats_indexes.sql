-- Statistiques du back-office par jour et par mois : filtrer sur une période sans parcourir toute la table
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_payments_status_updated_at ON payments (status, updated_at);
