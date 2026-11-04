-- Abonnés à l'infolettre. unsubscribe_token : secret aléatoire propre à chaque abonné, mis dans le lien de
-- désinscription (désinscrire par l'email permettrait à n'importe qui de désinscrire n'importe qui)
CREATE TABLE newsletter_subscribers (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email              VARCHAR(255) NOT NULL UNIQUE,
    unsubscribe_token  VARCHAR(64)  NOT NULL UNIQUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
