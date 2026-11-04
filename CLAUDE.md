# Contexte du projet – Boutique en ligne

Je construis une boutique en ligne pour APPRENDRE Spring Boot en profondeur.
Stack : Java 21, Spring Boot 4.1, Maven, PostgreSQL, Flyway, Spring Security (JWT puis Keycloak),
Spring AI 2.x, React + TypeScript, Docker, GitHub Actions.
Architecture : monolithe modulaire, un package par domaine (catalog, cart, order, review, payment, user, notification, agent),
sans dépendance circulaire entre packages (ex. review dépend de catalog et order, jamais l'inverse).

## Structure du dépôt

- `backend/` : API Spring Boot (package racine `com.boutique`), build avec le Maven Wrapper (`./mvnw`)
- `frontend/` : React 19 + TypeScript 7 + Vite 8 + Tailwind 4, React Router 8, TanStack Query 5, oxlint, Vitest
  (Node 24 installé dans `~/.local/opt/node` : `export PATH=$HOME/.local/opt/node/bin:$PATH`)
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

## Frontend

- `cd frontend && npm run dev` (http://localhost:5173) ; proxy Vite `/api` → `http://localhost:8080` : une seule origine,
  donc pas de CORS à ouvrir dans l'API (en production, Caddy fera le même relais)
- Code : guillemets simples, sans point-virgule, imports `import type` (verbatimModuleSyntax), pas d'enum TypeScript
- Couches : `api/` (fetch + `ApiError` lisant les ProblemDetail), `hooks/` (TanStack Query), `components/`, `pages/`
- Filtres du catalogue dans l'URL (`useSearchParams`) ; accessibilité : labels, `role="alert"`, `aria-live`, focus visible
- Textes venant des clients (avis) affichés comme texte React, jamais en HTML (pas de `dangerouslySetInnerHTML`)
- Session : jeton d'accès UNIQUEMENT en mémoire (`api/client.ts`, jamais localStorage) ; refresh token dans le cookie
  httpOnly `boutique_refresh` (SameSite=Strict, Path=/api/auth, Secure sauf profil dev) ; `restoreSession` au chargement ;
  renouvellement unique à la fois (sinon l'API croit à un vol et coupe toutes les sessions) ; 401 → refresh → rejeu
- `RequireAuth` pour les pages privées (confort d'affichage, l'API reste la vraie protection) ; `safeReturnPath`
  contre les redirections ouvertes (?retour=…)
- Paiement en ligne : redirection vers `checkoutUrl` du fournisseur ; au retour, on attend la confirmation du serveur
  (polling) ; en dev, `/paiement-simule/:référence` imite la page du fournisseur
- Design « Shop Africa » d'après les maquettes de l'utilisateur (`shop_africa.html`, `shop_africa_admin.html`) :
  jetons dans `index.css` (`brand` = indigo, `stone` = gris bleutés, `safran` boutons/prix, `bissap` alertes,
  `kola` stock/confirmations, `tint-*` fonds des rayons) ; police Bricolage Grotesque auto-hébergée (`@fontsource-variable`,
  aucun appel à Google Fonts) ; `Logo`, `Icon`, `PriceTag` (étiquette de prix), `lib/categories.ts` (emoji + couleur par rayon)
- Boutique : en-tête collant (recherche, rayons), étal d'accueil, panier en tiroir (`CartDrawer`, `<dialog>` natif),
  toasts (`ToastProvider` + `useToast`), localisation de livraison (`DeliveryLocation` : géolocalisation du navigateur,
  carte OpenStreetMap intégrée, message clair si refus), frise de suivi (`DeliveryTracking`)
- Back-office `/admin` : route à part, hors du `Layout` de la boutique (cadre `AdminLayout` : barre latérale indigo,
  icônes seules sur mobile), sous `RequireAuth` + `RequireAdmin` ; pages Vue d'ensemble, Produits, Ajouter un article
  (photos choisies avant l'enregistrement, envoyées juste après), Commandes (changement de statut limité aux
  `nextStatuses` de l'API), Paiements (espèces à récupérer), Analyse des ventes, Clients ; briques dans `components/admin/ui.tsx`
- Favoris (cœur, `FavoriteButton`, mise à jour optimiste), promotions (`/?promo=1`, prix barré), infolettre (accueil,
  `/desinscription?jeton=`), page de commande pilotée par les paramètres publics (`lib/checkout.ts`) ; admin : Livreurs,
  Infolettre, Paramètres, boutons « Exporter » (`apiDownload` : fichier récupéré avec le jeton puis enregistré)
- `vite.config.ts` : `API_URL` (facultatif) change la cible du proxy (défaut `http://localhost:8080`)
- Graphiques : SVG écrit à la main (`components/admin/SalesChart.tsx`), une couleur par série, info-bulle au survol ET
  au clavier, tableau des données à côté ; pas de bibliothèque de graphiques
- Tableaux qui défilent : conteneur `relative overflow-x-auto` (sinon les libellés `sr-only` élargissent la page)
- Envoi de fichiers : `FormData` passé tel quel à `apiRequest` (le navigateur écrit le Content-Type multipart)
- CI : job `frontend` (npm ci, lint, test, build)

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
- Jeton d'accès 15 min (claims `sub` = id utilisateur, `roles`) dans le corps de la réponse ; refresh token 7 jours opaque,
  envoyé UNIQUEMENT dans un cookie httpOnly, stocké haché (SHA-256), rotation à chaque usage, réutilisation détectée →
  toutes les sessions révoquées
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
- Commande = adresse de livraison obligatoire (GPS facultatif) ; `Delivery` suit le statut de la commande par appel
  direct (même transaction) ; pas de `@OneToOne` inverse sur `Order` (toujours chargé, provoquerait des N+1)
- Avis (package `review`) : uniquement si une commande DELIVERED contient le produit, une fois (UNIQUE en base) ;
  auteur affiché « Prénom N. » ; commentaires = texte non fiable (XSS côté frontend, injection de prompt pour l'agent)
- Événements : appel direct pour ce qui doit être dans la même transaction, `ApplicationEvent` pour les conséquences ;
  `OrderPaidEvent` → `@TransactionalEventListener(AFTER_COMMIT)` dans `notification` (rien n'est envoyé si rollback)
- Admin : `/api/admin/orders` (filtres `status`, `paymentMethod`, `q` via `OrderSpecifications`) renvoie
  `AdminOrderResponse` (client + `nextStatuses` pour l'acteur ADMIN) ; package `admin` = statistiques
  (`GET /api/admin/stats`, `/stats/sales?period=DAY|MONTH`) et clients (`GET /api/admin/customers`) calculés en base
  (projections, `date_trunc` à l'heure `Africa/Dakar`, périodes vides complétées en Java), jamais en chargeant toutes
  les commandes en mémoire ; un produit déjà commandé ne se supprime pas (clé étrangère → 409)
- Statistiques : panier moyen, ventes par catégorie, clients fidèles (≥ 2 commandes), espèces à récupérer
  (commandes en espèces ni livrées ni annulées) ; `GET /api/admin/orders?status=CREATED,PREPARING` accepte plusieurs statuts
- Recherche texte : `common/LikePatterns.contains` (échappe `%` et `_`) pour tous les LIKE

## Boutique : fiche produit, favoris, infolettre, livreurs, paramètres, exports

- Produit : `brand` (facultatif), `sku` (référence unique, `SHA-` + 8 caractères générés si vide, majuscules),
  `compareAtPrice` (prix barré > prix, vérifié par le code ET un `CHECK` en base) ; `discountPercent` calculé par le
  serveur (arrondi inférieur) ; `GET /api/products?promo=true` ; recherche sur nom, marque et référence
- Package `favorite` : table `favorites` (clé primaire composée, `ON DELETE CASCADE`), `PUT`/`DELETE /api/favorites/{id}`
  idempotents, `GET /api/favorites` et `/ids`
- Package `newsletter` : inscription publique limitée en débit, même réponse inscrit ou non (anti-énumération),
  `INSERT … ON CONFLICT DO NOTHING`, désinscription par jeton secret (POST, jamais un simple lien GET) ; message de
  bienvenue via `NotificationSender` (logs pour l'instant) ; `GET /api/admin/newsletter` sans le jeton
- Livreurs (`order/Courier`, `deliveries.courier_id`) : jamais supprimés, désactivés ; affectation
  `PUT /api/admin/orders/{id}/courier` refusée pour un livreur inactif ou une commande terminée (409) ; le client voit
  son livreur seulement une fois le colis expédié (`DeliveryResponse.forCustomer`)
- Package `store` : `store_settings` (une ligne, `id = 1`) ; `GET /api/store/settings` public, `PUT /api/admin/settings` ;
  appliqués par `OrderService.checkout` (paiement en ligne/espèces activé, plafond, villes desservies sans accents ni
  majuscules) ; frais de service figés dans `orders.service_fee` et inclus dans le total ; `order` dépend de `store`
- Exports CSV `GET /api/admin/exports/{products|orders|customers|newsletter}.csv` : `common/CsvWriter` (BOM UTF-8,
  `;`, CRLF, cellules commençant par `= + - @` préfixées d'une apostrophe contre l'injection de formule), pages de 500

## Photos des produits

- Table `product_images` (position 0 = principale, 8 max) ; `Product.images` avec `@BatchSize` (pas de N+1 sur une
  page) et `@OptimisticLock(excluded = true)` (une photo ne fait pas échouer un achat simultané)
- `POST /api/products/{id}/images` (multipart `file`, ADMIN, 5 Mo max → 413), `PUT …/{imageId}/main`, `DELETE …/{imageId}` ;
  lecture publique `GET /api/images/{uuid}.jpg` (cache 1 an, nom = UUID donc jamais modifié)
- `ImageProcessor` : premiers octets JPEG/PNG, dimensions lues avant décodage (8 000 px max), réduit à 1 600 px,
  réenregistré en JPEG sans métadonnées (EXIF/GPS supprimés) ; traitement et écriture HORS transaction
- `ImageStorage` (stratégie) : `LocalImageStorage` dans `boutique.catalog.images-dir` (`IMAGES_DIR`, défaut
  `./data/images`, ignoré par git ; tests : `target/test-images`) ; fichiers effacés APRÈS le commit
  (`ProductImagesDeletedEvent` + `@TransactionalEventListener`)

## Paiement

- `PaymentProvider` (pattern Stratégie : initier, vérifier, traiter un webhook) ; `boutique.payment.provider` =
  `fake` (dev/tests, refusé avec le profil prod) ou `paydunya` (clés `PAYDUNYA_*` dans `.env`, mode test = bac à sable)
- PayDunya écrit UNIQUEMENT d'après https://developers.paydunya.com/doc/FR/http_json : create invoice, confirm/{token},
  IPN en form-urlencoded (`data[hash]` = SHA-512 de la MasterKey, `data[invoice][token]`) ; tests avec MockRestServiceServer
- Statut « payé » : uniquement via webhook vérifié PUIS reconfirmé auprès de l'API du fournisseur, montant comparé à la
  commande ; jamais depuis la page de retour ; webhook idempotent ; réconciliation planifiée (`PaymentReconciliationJob`)
- Aucun appel réseau pendant une transaction ouverte (`PaymentService.initiate` utilise `TransactionTemplate`)
- Journal `payment_events` pour chaque étape ; espèces à la livraison (`CASH_ON_DELIVERY`) : préparée sans paiement,
  paiement « cash » enregistré à DELIVERED (événement `OrderDeliveredEvent` écouté de façon synchrone), refus à la porte
- Dev : `POST /api/dev/fake-payments/{référence}/paye|annule` simule la page de paiement et le webhook signé

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
