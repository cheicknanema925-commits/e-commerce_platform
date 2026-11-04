-- Marque (fabricant), référence interne unique (SKU) et prix barré (prix avant remise, facultatif)
ALTER TABLE products
    ADD COLUMN brand            VARCHAR(80),
    ADD COLUMN sku              VARCHAR(40),
    ADD COLUMN compare_at_price NUMERIC(12, 2);

-- Les produits existants reçoivent une référence lisible : SHA-0001, SHA-0002…
UPDATE products SET sku = 'SHA-' || lpad(id::text, 4, '0');

ALTER TABLE products
    ALTER COLUMN sku SET NOT NULL,
    ADD CONSTRAINT uk_products_sku UNIQUE (sku),
    -- Une promotion annonce un prix barré plus élevé que le prix de vente, jamais l'inverse
    ADD CONSTRAINT ck_products_compare_at_price CHECK (compare_at_price IS NULL OR compare_at_price > price);
