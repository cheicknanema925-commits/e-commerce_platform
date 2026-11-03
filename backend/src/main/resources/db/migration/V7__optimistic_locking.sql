-- Verrouillage optimiste : chaque UPDATE devient « ... SET version = version + 1 WHERE id = ? AND version = ? ».
-- Si quelqu'un a modifié la ligne entre-temps, la version ne correspond plus : 0 ligne mise à jour, l'opération échoue.
-- products : deux clients qui achètent le dernier article en même temps.
-- orders   : un client qui annule pendant que le paiement confirme la même commande.
ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orders   ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
