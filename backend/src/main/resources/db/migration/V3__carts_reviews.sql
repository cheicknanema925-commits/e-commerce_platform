-- Un seul panier par client (UNIQUE sur user_id)
CREATE TABLE carts (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL UNIQUE REFERENCES users (id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Un produit n'apparaît qu'une fois par panier : on augmente la quantité
CREATE TABLE cart_items (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id     BIGINT  NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id  BIGINT  NOT NULL REFERENCES products (id),
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

-- Un client ne note un produit qu'une seule fois
CREATE TABLE reviews (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id),
    product_id  BIGINT        NOT NULL REFERENCES products (id),
    rating      INTEGER       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     VARCHAR(2000),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (user_id, product_id)
);

CREATE INDEX idx_reviews_product_id ON reviews (product_id);
