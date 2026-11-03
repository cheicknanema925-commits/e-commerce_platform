-- "user" et "order" sont des mots réservés en SQL : tables au pluriel
CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    full_name   VARCHAR(150) NOT NULL,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER', 'ADMIN')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT         NOT NULL REFERENCES users (id),
    status        VARCHAR(20)    NOT NULL
        CHECK (status IN ('CREATED', 'PAID', 'PREPARING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    total_amount  NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders (user_id);

-- Nom et prix copiés au moment de l'achat : la commande ne change pas si le produit change
CREATE TABLE order_items (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id      BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    BIGINT         NOT NULL REFERENCES products (id),
    product_name  VARCHAR(100)   NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL CHECK (unit_price > 0),
    quantity      INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
