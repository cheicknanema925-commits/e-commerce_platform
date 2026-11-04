-- Favoris : « tel client aime tel produit », une seule fois (clé primaire sur le couple).
-- Un produit ou un compte supprimé emporte ses favoris (ON DELETE CASCADE) : un favori ne bloque jamais une suppression.
CREATE TABLE favorites (
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id  BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, product_id)
);

CREATE INDEX idx_favorites_product_id ON favorites (product_id);
