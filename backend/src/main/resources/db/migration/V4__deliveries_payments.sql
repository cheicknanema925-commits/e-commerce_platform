-- Une seule livraison par commande (UNIQUE sur order_id)
CREATE TABLE deliveries (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id      BIGINT        NOT NULL UNIQUE REFERENCES orders (id),
    status        VARCHAR(20)   NOT NULL CHECK (status IN ('PENDING', 'PREPARING', 'SHIPPED', 'DELIVERED')),
    address_line  VARCHAR(255)  NOT NULL,
    city          VARCHAR(100)  NOT NULL,
    phone         VARCHAR(30)   NOT NULL,
    latitude      NUMERIC(9, 6),
    longitude     NUMERIC(9, 6),
    shipped_at    TIMESTAMPTZ,
    delivered_at  TIMESTAMPTZ
);

-- Plusieurs tentatives de paiement possibles par commande (ex. échec Wave puis Orange Money)
CREATE TABLE payments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id            BIGINT         NOT NULL REFERENCES orders (id),
    provider            VARCHAR(30)    NOT NULL,
    provider_reference  VARCHAR(100)   UNIQUE,
    amount              NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    status              VARCHAR(20)    NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED')),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
