-- Étape 8 : agent IA

-- Recherche par similarité : extension pgvector (déjà présente dans l'image pgvector/pgvector)
CREATE EXTENSION IF NOT EXISTS vector;

-- Mémoire des conversations : schéma officiel de Spring AI pour PostgreSQL (schema-postgresql.sql du module
-- spring-ai-model-chat-memory-repository-jdbc 2.0.2), créé ici par Flyway comme le reste du schéma
CREATE TABLE spring_ai_chat_memory (
    conversation_id  VARCHAR(36) NOT NULL,
    content          TEXT        NOT NULL,
    type             VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp"      TIMESTAMP   NOT NULL,
    sequence_id      BIGINT      NOT NULL
);
CREATE INDEX spring_ai_chat_memory_conversation_id_timestamp_idx ON spring_ai_chat_memory (conversation_id, "timestamp");
CREATE INDEX spring_ai_chat_memory_conversation_id_sequence_id_idx ON spring_ai_chat_memory (conversation_id, sequence_id);

-- Index sémantique du catalogue (format de PgVectorStore) : une ligne par produit, id = « product-<id> »,
-- vecteur de 768 nombres (modèle d'embeddings nomic-embed-text), distance cosinus, index HNSW
CREATE TABLE vector_store (
    id         TEXT PRIMARY KEY,
    content    TEXT,
    metadata   JSON,
    embedding  VECTOR(768)
);
CREATE INDEX spring_ai_vector_index ON vector_store USING hnsw (embedding vector_cosine_ops);

-- Journal de chaque appel d'outil par un agent : qui, quel outil, quels arguments, quel résultat, combien de temps
CREATE TABLE agent_tool_calls (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    agent        VARCHAR(10)   NOT NULL CHECK (agent IN ('CLIENT', 'ADMIN')),
    tool         VARCHAR(60)   NOT NULL,
    arguments    VARCHAR(1000),
    outcome      VARCHAR(10)   NOT NULL CHECK (outcome IN ('OK', 'REFUSED', 'ERROR')),
    detail       VARCHAR(500),
    duration_ms  INTEGER       NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_agent_tool_calls_user_created ON agent_tool_calls (user_id, created_at);

-- Actions proposées par un agent : RIEN n'est exécuté tant que l'utilisateur n'a pas cliqué sur « Confirmer ».
-- steps : liste JSON des étapes (ex. ajouter 2 sacs, puis 1 lampe) ; results : compte rendu après exécution
CREATE TABLE agent_proposals (
    id          UUID          PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    agent       VARCHAR(10)   NOT NULL CHECK (agent IN ('CLIENT', 'ADMIN')),
    steps       TEXT          NOT NULL,
    status      VARCHAR(12)   NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    results     TEXT,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ   NOT NULL,
    decided_at  TIMESTAMPTZ
);
CREATE INDEX idx_agent_proposals_user ON agent_proposals (user_id, created_at);
