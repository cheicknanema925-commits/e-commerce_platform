# Boutique Spring Boot

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
