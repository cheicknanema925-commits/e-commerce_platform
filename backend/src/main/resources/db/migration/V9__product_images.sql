-- Photos des produits : le fichier est rangé hors de la base (disque, plus tard stockage objet), la base garde
-- son nom. position 0 = photo principale (celle du catalogue). Produit supprimé = ses photos aussi.
CREATE TABLE product_images (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id  BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    filename    VARCHAR(64)  NOT NULL UNIQUE,
    position    INTEGER      NOT NULL CHECK (position >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);
