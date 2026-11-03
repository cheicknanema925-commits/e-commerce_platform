# Boutique Spring Boot

[![CI](https://github.com/cheicknanema925-commits/e-commerce_platform/actions/workflows/ci.yml/badge.svg)](https://github.com/cheicknanema925-commits/e-commerce_platform/actions/workflows/ci.yml)

Boutique en ligne complète construite avec Spring Boot : API sécurisée, paiement mobile money, agent IA avec outils, frontend React, déploiement Docker et CI/CD.

> Projet en cours de construction.

## Stack

Java 21 · Spring Boot 4.1 · Maven · PostgreSQL · Flyway · Spring Security / Keycloak · Spring AI · React + TypeScript · Docker · GitHub Actions

## Lancer le projet en local

Prérequis : JDK 21 et Docker.

```bash
cp .env.example .env          # puis choisir un mot de passe dans .env
docker compose up -d          # PostgreSQL
cd backend
./mvnw spring-boot:run
```

Vérifier que l'API répond :

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

Documentation interactive de l'API (profil dev) : http://localhost:8080/swagger-ui.html

## Authentification

JWT (15 min) + refresh token révocable (7 jours). Le premier administrateur est créé au démarrage à partir de
`ADMIN_EMAIL` / `ADMIN_PASSWORD` dans `.env`.

```bash
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"email":"awa@example.com","fullName":"Awa Diop","password":"MotDePasse-Solide-2026"}'
curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"awa@example.com","password":"MotDePasse-Solide-2026"}'
curl localhost:8080/api/users/me -H "Authorization: Bearer <accessToken>"
```

### Connexion par Keycloak (OpenID Connect)

L'API accepte aussi les jetons du realm Keycloak `boutique` (rôles `CUSTOMER` / `ADMIN`). Le compte local est créé
automatiquement à la première requête.

```bash
docker compose --profile keycloak up -d --wait   # Keycloak sur http://localhost:8180
./keycloak/demo-users.sh                         # comptes de démo : fatou (CUSTOMER), mariama (ADMIN)
TOKEN=$(curl -s -X POST localhost:8180/realms/boutique/protocol/openid-connect/token \
  -d grant_type=password -d client_id=boutique-cli -d username=fatou -d "password=<KEYCLOAK_DEMO_PASSWORD>" | jq -r .access_token)
curl localhost:8080/api/users/me -H "Authorization: Bearer $TOKEN"
```

Le client `boutique-cli` (mot de passe direct) sert uniquement aux tests en ligne de commande ; le frontend utilisera
le client `boutique-frontend` (Authorization Code + PKCE).

## Tests

```bash
cd backend
./mvnw verify    # tests unitaires, @WebMvcTest, @DataJpaTest, Testcontainers + couverture JaCoCo
```

Rapport de couverture : `backend/target/site/jacoco/index.html`. La CI GitHub Actions lance la même commande à chaque pull request.
