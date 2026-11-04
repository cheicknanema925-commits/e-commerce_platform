-- Livreurs. On ne les supprime jamais (les anciennes livraisons garderaient un trou) : on les désactive.
CREATE TABLE couriers (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(30)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Livreur affecté à une livraison (facultatif tant que la commande n'est pas partie)
ALTER TABLE deliveries ADD COLUMN courier_id BIGINT REFERENCES couriers (id);
CREATE INDEX idx_deliveries_courier_id ON deliveries (courier_id);
