# Contexte du projet – Boutique en ligne

Je construis une boutique en ligne pour APPRENDRE Spring Boot en profondeur.
Stack : Java 21, Spring Boot 4.1, Maven, PostgreSQL, Flyway, Spring Security (JWT puis Keycloak),
Spring AI 2.x, React + TypeScript, Docker, GitHub Actions.
Architecture : monolithe modulaire, un package par domaine (catalog, cart, order, payment, user, agent).

## Structure du dépôt

- `backend/` : API Spring Boot (package racine `com.boutique`), build avec le Maven Wrapper (`./mvnw`)
- `frontend/` : React + TypeScript + Vite + Tailwind (à partir de l'étape 7)
- `.env.example` : modèle des variables d'environnement ; le vrai `.env` n'est jamais commité

## Commandes utiles

```bash
cd backend
./mvnw spring-boot:run      # démarrer l'API sur http://localhost:8080
./mvnw test                 # lancer les tests
./mvnw verify               # build complet + tests
```

## Règles pour toi

- Explique toujours le concept en mots simples AVANT de donner du code.
- Donne le code par petits morceaux, avec le chemin de chaque fichier.
- Chaque fonctionnalité vient avec ses tests (JUnit 5, Mockito, Testcontainers).
- Sécurité par défaut : aucun secret dans le code, validation des entrées, moindre privilège.
- N'invente pas d'API : si tu n'es pas sûr d'une classe ou d'une version, dis-le.
- Termine par 3 questions pour vérifier que j'ai compris.
