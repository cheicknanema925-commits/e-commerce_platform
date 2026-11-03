-- Mode de paiement choisi à la commande : en ligne (PayDunya : Wave, Orange Money, carte…) ou en espèces à la livraison
ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'ONLINE'
        CHECK (payment_method IN ('ONLINE', 'CASH_ON_DELIVERY'));

-- Verrouillage optimiste : deux notifications identiques traitées en même temps ne peuvent pas valider deux fois
ALTER TABLE payments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Journal de chaque événement de paiement (création, notification reçue ou rejetée, montant incohérent…)
CREATE TABLE payment_events (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id  BIGINT        REFERENCES payments (id) ON DELETE CASCADE,
    provider    VARCHAR(30)   NOT NULL,
    type        VARCHAR(30)   NOT NULL,
    detail      VARCHAR(1000),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_events_payment_id ON payment_events (payment_id);
