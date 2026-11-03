CREATE TABLE categories (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    slug  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE products (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id  BIGINT         NOT NULL REFERENCES categories (id),
    name         VARCHAR(100)   NOT NULL,
    description  VARCHAR(1000),
    price        NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    stock        INTEGER        NOT NULL CHECK (stock >= 0),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category_id ON products (category_id);

-- Catégories de référence de la boutique
INSERT INTO categories (name, slug) VALUES
    ('Mode', 'mode'),
    ('Beauté', 'beaute'),
    ('Électronique', 'electronique'),
    ('Maison', 'maison'),
    ('Alimentation', 'alimentation');
