-- Paramètres de la boutique : UNE seule ligne (id = 1 imposé par la contrainte)
CREATE TABLE store_settings (
    id                      SMALLINT      PRIMARY KEY CHECK (id = 1),
    store_name              VARCHAR(100)  NOT NULL,
    support_email           VARCHAR(255)  NOT NULL,
    online_payment_enabled  BOOLEAN       NOT NULL,
    cod_enabled             BOOLEAN       NOT NULL,
    -- Plafond d'une commande payée en espèces (NULL = sans plafond)
    cod_max_amount          NUMERIC(12, 2) CHECK (cod_max_amount IS NULL OR cod_max_amount > 0),
    cod_fee                 NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (cod_fee >= 0),
    -- Villes où le paiement en espèces est possible, séparées par des virgules ('' = partout)
    cod_zones               VARCHAR(500)  NOT NULL DEFAULT '',
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now()
);

INSERT INTO store_settings (id, store_name, support_email, online_payment_enabled, cod_enabled)
VALUES (1, 'Shop Africa', 'contact@shopafrica.sn', TRUE, TRUE);

-- Frais de service figés dans la commande (espèces à la livraison) ; le total les inclut
ALTER TABLE orders ADD COLUMN service_fee NUMERIC(12, 2) NOT NULL DEFAULT 0;
