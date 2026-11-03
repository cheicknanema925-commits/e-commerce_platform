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
cp .env.example .env        # une seule fois, puis mettre un vrai mot de passe
docker compose up -d        # démarrer PostgreSQL (port 5432, uniquement en local)
docker compose down         # l'arrêter (les données restent dans le volume postgres-data)

cd backend
./mvnw spring-boot:run      # démarrer l'API sur http://localhost:8080 (profil dev activé par le pom.xml)
./mvnw test                 # lancer les tests
./mvnw verify               # build complet + tests
java -jar target/boutique-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod   # comme en production
```

## Profils

- `application.yml` : configuration commune, sûre par défaut (jamais de stack trace dans les réponses)
- `application-dev.yml` : lit `../.env`, messages d'erreur détaillés, requêtes SQL dans les logs
- `application-prod.yml` : logs JSON (format ECS)
- Spring Boot 4 : les propriétés d'erreur sont `spring.web.error.*` (`server.error.*` est déprécié)

## Tests et CI

- Unitaires (JUnit + Mockito, sans Spring) pour la logique métier
- `@WebMvcTest` + `@MockitoBean` pour les contrôleurs (codes HTTP, validation, format des erreurs)
- `@DataJpaTest` + Testcontainers pour les repositories (rollback automatique après chaque test)
- `@SpringBootTest` + Testcontainers pour les parcours complets
- JaCoCo : `./mvnw verify` → `backend/target/site/jacoco/index.html`
- CI : `.github/workflows/ci.yml` lance `./mvnw -B verify` sur chaque pull request et sur main
- Swagger (springdoc 3.x) : désactivé par défaut, activé en dev → `/swagger-ui.html`

## Sécurité

- `security/SecurityConfig` : API sans état, JWT HS256 vérifiés par le resource server de Spring Security
- Jeton d'accès 15 min (claims `sub` = id utilisateur, `roles`), refresh token 7 jours opaque, stocké haché (SHA-256),
  rotation à chaque usage, réutilisation détectée → toutes les sessions révoquées
- Public : `GET` catalogue, `/api/auth/**`, health, docs ; `/api/admin/**` = ADMIN ; écritures du catalogue = `@PreAuthorize` ADMIN ;
  tout le reste = authentifié
- Deux émetteurs acceptés (`JwtIssuerAuthenticationManagerResolver`, choix par le claim `iss`) : nos jetons HS256 (rôles
  dans `roles`) et Keycloak RS256 via JWKS (rôles dans `realm_access.roles`, audience `boutique-api` obligatoire) ;
  `KEYCLOAK_ISSUER_URI` vide = Keycloak désactivé
- Identité TOUJOURS lue dans le jeton (`CurrentUserResolver.userId(jwt)`), jamais dans l'URL ou le corps (protection IDOR) ;
  jeton Keycloak → compte local créé à la première visite (`users.external_id` = `sub`), relié à un compte existant
  seulement si `email_verified` est vrai
- Keycloak (dev) : `docker compose --profile keycloak up -d --wait` puis `./keycloak/demo-users.sh` (fatou CUSTOMER,
  mariama ADMIN) ; realm importé depuis `keycloak/realm/realm-boutique.json` ; console http://localhost:8180
- Connexion : verrouillage après 5 échecs (15 min), audit dans `login_events`, même message d'erreur quel que soit le cas
- Connexion ET inscription : 10 requêtes/min par IP (`AuthRateLimitFilter`, en mémoire) ; tous les champs d'entrée ont un `@Size`
- Audit OWASP et risques planifiés : `docs/audit-securite.md` (à relire aux étapes 7, 9 et 10)
- BCrypt refuse plus de 72 octets : vérifié avant chaque appel au `PasswordEncoder`
- `.env` : `JWT_SECRET` (32 caractères min, sinon l'appli refuse de démarrer), `ADMIN_EMAIL`, `ADMIN_PASSWORD` (premier admin)
- Tests : `jwt()` (spring-security-test) pour les tests de tranche, vrais jetons via `/api/auth/login` pour l'intégration ;
  clé de test dans `src/test/resources/config/application.yml` (chargée uniquement pendant les tests)
- Spring Security 7 : `WWW-Authenticate` contient aussi `resource_metadata` (RFC 9728)

## Panier et commandes

- Logique métier dans les entités et services (`Cart.addItem`, `Order.changeStatus`), jamais dans les contrôleurs
- Machine à états : tableau `OrderStatus.TRANSITIONS` (statut → statuts suivants → acteurs autorisés : CUSTOMER, ADMIN,
  PAYMENT_PROVIDER) ; `PAID` uniquement par le paiement (`OrderService.markPaid`, appelé par le webhook à l'étape 6)
- Prix TOUJOURS relu en base (aucun prix dans les requêtes), figé dans `OrderItem` à la commande ; stock réservé à la
  commande, rendu à l'annulation ; panier : max 99 par produit, stock vérifié à l'ajout ET à la commande
- `@Version` sur `Product` et `Order` (verrouillage optimiste) → `OptimisticLockingFailureException` → 409
  (`ConcurrentPurchaseIntegrationTests` : deux clients, un seul dernier article)
- Id dans l'URL (`/api/orders/{id}`) : `findByIdAndUserId` → 404 si la commande appartient à un autre (IDOR)
- Liste paginée avec relations : page d'abord, puis `fetchItems(ids)` (jamais de JOIN FETCH paginé sur une collection)
- Tests : `jwt().jwt(t -> t.issuer("boutique-api").subject(id))` simule un utilisateur local

## Erreurs

- `common/GlobalExceptionHandler` (`@RestControllerAdvice`) : toutes les erreurs au format `ProblemDetail` (RFC 9457)
- Lever une sous-classe de `NotFoundException` (404) ou `ConflictException` (409), jamais `@ResponseStatus`
- 500 : message générique + `errorId`, le détail technique uniquement dans les logs
- Spring Boot 4 : `@WebMvcTest`/`@AutoConfigureMockMvc` dans `org.springframework.boot.webmvc.test.autoconfigure`,
  `PropertyReferenceException` dans `org.springframework.data.core`

## Base de données

- Schéma géré uniquement par Flyway : `backend/src/main/resources/db/migration/V<n>__<description>.sql`
- Ne jamais modifier une migration déjà appliquée : toute évolution = nouvelle migration
- Hibernate en `ddl-auto: validate`, `open-in-view: false`, relations `@ManyToOne`/`@OneToOne` en `LAZY`
- Listes avec relations : `JOIN FETCH` ou `@EntityGraph` pour éviter le N+1 (vérifié par `OrderServiceIntegrationTests` avec les statistiques Hibernate)
- Écritures dans des méthodes `@Transactional` : rollback automatique sur `RuntimeException`
- Tests d'intégration : Testcontainers (`TestcontainersConfiguration`), même image que docker-compose

## Règles pour toi

- Explique toujours le concept en mots simples AVANT de donner du code.
- Donne le code par petits morceaux, avec le chemin de chaque fichier.
- Chaque fonctionnalité vient avec ses tests (JUnit 5, Mockito, Testcontainers).
- Sécurité par défaut : aucun secret dans le code, validation des entrées, moindre privilège.
- N'invente pas d'API : si tu n'es pas sûr d'une classe ou d'une version, dis-le.
- Termine par 3 questions pour vérifier que j'ai compris.
