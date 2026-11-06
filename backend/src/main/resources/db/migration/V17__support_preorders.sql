-- Demandes au service client : ouvertes par le client (site ou assistant), traitées dans le back-office
CREATE TABLE support_tickets (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Commande concernée (facultative) ; le ticket reste si la commande disparaît
    order_id    BIGINT        REFERENCES orders (id) ON DELETE SET NULL,
    category    VARCHAR(20)   NOT NULL CHECK (category IN ('ORDER', 'DELIVERY', 'PAYMENT', 'PRODUCT', 'OTHER')),
    subject     VARCHAR(120)  NOT NULL,
    message     VARCHAR(2000) NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ANSWERED', 'CLOSED')),
    reply       VARCHAR(2000),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    replied_at  TIMESTAMPTZ
);
CREATE INDEX idx_support_tickets_user_id ON support_tickets (user_id);
CREATE INDEX idx_support_tickets_status ON support_tickets (status, created_at);

-- Pré-commandes : « prévenez-moi quand ce produit en rupture revient ». Rien n'est payé ni réservé.
CREATE TABLE pre_orders (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id   BIGINT      NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity     INT         NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    status       VARCHAR(20) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'NOTIFIED', 'CANCELLED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    notified_at  TIMESTAMPTZ
);
-- Une seule demande EN ATTENTE par client et par produit (les anciennes, notifiées ou annulées, restent)
CREATE UNIQUE INDEX uq_pre_orders_waiting ON pre_orders (user_id, product_id) WHERE status = 'WAITING';
CREATE INDEX idx_pre_orders_product_waiting ON pre_orders (product_id) WHERE status = 'WAITING';
