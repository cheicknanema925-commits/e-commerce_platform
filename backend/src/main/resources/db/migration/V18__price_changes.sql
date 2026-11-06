-- Journal des changements de prix (assistant admin) : ancien et nouveau prix, auteur, lot, fin et annulation.
-- Permet d'annuler un lot entier et de rétablir les prix à la fin d'une promotion.
CREATE TABLE price_changes (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_id              UUID          NOT NULL,
    product_id            BIGINT        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    author_id             BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    kind                  VARCHAR(20)   NOT NULL CHECK (kind IN ('PROMOTION', 'PRICE')),
    old_price             NUMERIC(12,2) NOT NULL,
    old_compare_at_price  NUMERIC(12,2),
    new_price             NUMERIC(12,2) NOT NULL CHECK (new_price > 0),
    new_compare_at_price  NUMERIC(12,2),
    ends_at               TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    reverted_at           TIMESTAMPTZ,
    revert_reason         VARCHAR(20)   CHECK (revert_reason IN ('UNDO', 'EXPIRED', 'OVERRIDDEN')),
    -- Même si le code était contourné : une promotion a toujours une date de fin
    CONSTRAINT promotion_has_end CHECK (kind <> 'PROMOTION' OR ends_at IS NOT NULL)
);
CREATE INDEX idx_price_changes_batch ON price_changes (batch_id);
CREATE INDEX idx_price_changes_product ON price_changes (product_id);
-- Promotions encore actives : lues chaque minute par la tâche d'expiration
CREATE INDEX idx_price_changes_active_promotions ON price_changes (ends_at) WHERE kind = 'PROMOTION' AND reverted_at IS NULL;
