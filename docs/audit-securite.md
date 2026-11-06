# Audit de sécurité – OWASP Top 10 (étape 4, revu aux étapes 7 et 8)

- **Date** : 3 novembre 2026
- **Périmètre** : API backend (Spring Boot 4.1.2, Spring Security 7.1.2), branche `main` après l'étape 4A
- **Référentiel** : OWASP Top 10, édition 2021
- **Méthode** : relecture du code, attaques réelles contre l'API lancée en local (`curl`), puis un test automatisé
  de non-régression pour chaque point vérifié

## Résumé

| Résultat | Nombre |
|---|---|
| Faille critique | 0 |
| Failles corrigées pendant l'audit | 2 |
| Risques acceptés ou planifiés dans une étape suivante | 8 |

## Failles et risques, classés par gravité

| # | Gravité | OWASP | Constat | Correction | État |
|---|---|---|---|---|---|
| 1 | Élevée en production | A02 | Pas de HTTPS ni de HSTS : les jetons circulent en clair sur le réseau | Reverse proxy Caddy avec HTTPS automatique | Planifié (étape 9) |
| 2 | Élevée en production | A04 | Derrière un reverse proxy, `getRemoteAddr()` renvoie l'IP du proxy : la limite de débit bloquerait tous les clients en même temps | Lire `X-Forwarded-For` uniquement depuis le proxy de confiance | Planifié (étape 9) |
| 3 | Moyenne | A09 | Un email de plusieurs Mo à la connexion faisait échouer l'écriture du journal d'audit : tentative **non tracée** et réponse 409 trompeuse | `@Size` sur `LoginRequest` et `RefreshTokenRequest` : rejet en 400 avant la base | **Corrigé** |
| 4 | Moyenne | A04 / A07 | Inscription sans limite : 15 comptes créés en une seconde depuis la même IP (spam, énumération des emails en masse) | `AuthRateLimitFilter` couvre aussi `/api/auth/register` (10 requêtes/min/IP) | **Corrigé** |
| 5 | Moyenne | A07 | Le refresh token est renvoyé dans le JSON, donc lisible par le JavaScript en cas de faille XSS | Cookie `httpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/auth` | **Corrigé** (étape 7) |
| 6 | Moyenne | A06 / A08 | Dépendances et actions GitHub non surveillées ; actions référencées par version, pas par SHA | Dependabot, CodeQL, épinglage des actions | Planifié (étape 10) |
| 7 | Faible | A07 | L'inscription révèle qu'un email existe déjà (409) | Atténué par la limite de débit ; correction complète : vérification par email avec une réponse identique dans tous les cas | Accepté pour l'instant |
| 8 | Faible | A04 | Le verrouillage peut être déclenché contre un tiers (5 essais avec son email) : déni de service ciblé de 15 minutes | Délai progressif, verrouillage par couple (compte, IP), CAPTCHA | Accepté (compromis) |
| 9 | Faible | A07 | Un jeton d'accès reste valable jusqu'à 15 minutes après la déconnexion | Durée courte ; liste de révocation si besoin | Accepté (choix d'architecture) |
| 10 | Info | A05 | CORS non configuré : tout appel venant d'un autre domaine est refusé | Frontend et API servis par la même origine (proxy Vite, puis Caddy) : aucun CORS à ouvrir | **Sans objet** (étape 7) |
| 11 | Faible | A04 | Photos des produits stockées sur le disque du serveur : perdues si le conteneur est recréé sans volume | Volume Docker sauvegardé, ou stockage objet (S3, MinIO) via `ImageStorage` | Planifié (étape 9) |

## Contrôles vérifiés

| OWASP | Contrôle | Preuve |
|---|---|---|
| A01 | IDOR : l'identité du client vient du jeton, jamais de la requête | `AccessControlIntegrationTests.customerSeesOnlyOwnOrders` |
| A01 | Rôles : 401 sans jeton, 403 sans le bon rôle, routes admin protégées deux fois (URL + `@PreAuthorize`) | `ProductControllerTest`, `AccessControlIntegrationTests` |
| A01 | Affectation de masse : `"role":"ADMIN"` envoyé à l'inscription est ignoré | `registerAlwaysCreatesCustomerEvenIfAdminRoleIsSent` |
| A02 | Mots de passe BCrypt ; clé JWT de 256 bits hors du code ; refresh tokens stockés hachés (SHA-256) | `SecurityConfig`, `AuthService` |
| A03 | Aucune requête SQL construite par concaténation (Spring Data, JPQL paramétré) ; tri sur un champ inconnu → 400 | `sortingOnUnknownPropertyReturns400` |
| A05 | En-têtes `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store` | `responsesCarrySecurityHeaders` |
| A05 | Actuator : seul `/actuator/health` est exposé, sans détails ; Swagger désactivé par défaut ; jamais de stack trace | `ApiDocumentationTests`, `ProductControllerTest` |
| A07 | Jeton `alg: none`, jeton falsifié et jeton expiré refusés (401) | `unsignedAlgNoneTokenIsRejected`, `tamperedTokenIsRejected`, `expiredTokenIsRejected` |
| A07 | Verrouillage après 5 échecs ; même message pour email inconnu, mauvais mot de passe ou compte verrouillé ; temps de réponse égalisé | `accountIsLockedAfterFiveFailedAttempts`, `unknownEmailAndWrongPasswordGiveTheSameAnswer` |
| A07 | Rotation du refresh token, avec révocation de toutes les sessions en cas de réutilisation | `refreshTokenIsRotatedAndReuseRevokesEverything` |
| A09 | Journal des connexions (`login_events`) consultable par les ADMIN | `adminCanReadLoginAuditLog` |
| A10 | Aucun appel sortant pour l'instant | À revoir aux étapes 6 (paiement) et 8 (agent IA) |

## Revue de l'étape 7 : envoi de photos (back-office)

Un fichier envoyé est une entrée non fiable, même quand il vient d'un admin (compte volé, photo piégée reçue d'un
fournisseur). Contrôles en place et leur preuve :

| OWASP | Contrôle | Preuve |
|---|---|---|
| A01 | Envoi, suppression, choix de la photo principale : ADMIN uniquement (401 / 403 sinon) ; lecture publique | `onlyAnAdminCanUploadAPhoto` |
| A03 / A08 | Format reconnu aux premiers octets (JPEG ou PNG), jamais au nom ni au `Content-Type` ; SVG et HTML déguisés refusés (400) | `fileThatIsNotAnImageIsRefusedWhateverItsName`, `fileThatIsNotAnImageIsRefusedEvenWithAnImageName` |
| A04 | Taille limitée à 5 Mo (413) ; dimensions lues AVANT le décodage, 8 000 px maximum (bombe de décompression) ; 8 photos par produit (409) | `decompressionBombIsRefusedBeforeDecoding`, `ninthPhotoIsRefused`, `GlobalExceptionHandlerTest` |
| A04 | Image redessinée et réenregistrée en JPEG : métadonnées EXIF (position GPS du téléphone) supprimées | `exifMetadataSuchAsGpsPositionIsRemoved` |
| A01 / A05 | Nom de fichier choisi par le serveur (UUID) ; lecture limitée au motif `uuid.jpg`, chemin vérifié dans le dossier (pas de `../`) | `unexpectedFileNamesAreNeverLookedUpOnDisk` |
| A05 | Servies avec `Content-Type: image/jpeg` et `X-Content-Type-Options: nosniff` : le navigateur ne peut pas les interpréter comme une page | Vérifié avec curl |
| A04 | Fichiers effacés seulement APRÈS le commit : une suppression refusée (produit déjà commandé) garde ses photos | `refusedProductDeletionKeepsItsPhotos`, `deletingAPhotoAlsoDeletesItsFile` |

## Revue de l'étape 7 : favoris, infolettre, livreurs, paramètres, exports

| OWASP | Contrôle | Preuve |
|---|---|---|
| A01 | Favoris : identité lue dans le jeton, chaque client ne voit que les siens (IDOR) | `favoritesArePrivate` |
| A04 / A07 | Infolettre : même réponse que l'adresse soit inscrite ou non (pas d'énumération), débit limité comme la connexion | `subscribingTwiceGivesTheSameAnswerAndASingleSubscription` |
| A01 | Désinscription uniquement avec un jeton aléatoire de 256 bits, jamais avec l'email ; jeton absent de l'API admin | `unsubscribeWorksWithTheSecretTokenOnly`, `adminSeesSubscribersWithoutTheirToken` |
| A04 | Paramètres de paiement appliqués par le serveur à chaque commande (masquer un bouton ne suffit pas) | `StoreSettingsIntegrationTests` |
| A03 | Exports CSV : injection de formule neutralisée (`=`, `+`, `-`, `@` préfixés d'une apostrophe) | `CsvWriterTest.formulaInjectionIsNeutralized`, `exportsAreCsvFilesForExcel` |
| A01 | Livreurs, paramètres, abonnés, exports : réservés à l'ADMIN (403 pour un client) | `customerCannotManageCouriers`, `exportsAreCsvFilesForExcel` |

## Revue de l'étape 8 : assistants IA (OWASP Top 10 pour les applications LLM, édition 2025)

Principe : le modèle est traité comme un **utilisateur non fiable** qui écrit du texte. Il ne voit que les outils de
son assistant, il ne peut que PROPOSER une action, et les limites sont dans le code (jamais seulement dans le prompt).
Chaque garde-fou a été retiré une fois volontairement pour vérifier qu'un test échoue sans lui (tests de mutation).

| OWASP LLM | Contrôle | Preuve |
|---|---|---|
| LLM01 Injection de prompt | Avis clients transmis comme citations encadrées et marquées « données non fiables », fausses balises neutralisées ; même un modèle qui « obéit » à l'avis ne peut rien exécuter (outil absent, limite de prix, confirmation) | `instructionsHiddenInAReviewStayQuotedAndCannotActAlone`, `instructionHiddenInAReviewCannotDiscountTheCatalog` |
| LLM02 Divulgation d'informations | Identité lue dans le jeton et passée aux outils par le `ToolContext` (invisible pour le modèle) : aucun paramètre « client » ; commande ou proposition d'un autre client = 404 | `customerOrdersAreNeverThoseOfSomeoneElse`, `anotherCustomerCannotConfirmTheProposal` |
| LLM05 Sortie mal gérée | Texte du modèle affiché comme texte React (jamais en HTML ni en Markdown) ; montants cités vérifiés contre ceux lus en base (`AmountVerifier`) ; fiches et cartes d'action construites par le serveur | `inventedPriceIsFlagged`, `inventedRevenueIsFlagged`, `AmountVerifierTest` |
| LLM06 Autonomie excessive | Deux assistants séparés (outils, route, mémoire) ; outil inconnu ou d'un autre assistant refusé et journalisé ; toute écriture = proposition exécutée au CLIC (réservation atomique, double clic → 409, expiration 15 min) ; une proposition admin ne se confirme pas par la route client | `clientAgentCannotRunAnAdminTool`, `addToCartWaitsForTheCustomerClick`, `adminProposalCannotBeConfirmedThroughTheClientRoute`, `customerCannotReachTheAdminAgent` |
| LLM06 Autonomie excessive (prix) | Limites dans `PricingService` : prix > 0, remise ≤ 70 %, date de fin obligatoire (≤ 90 jours, et `CHECK` en base), hausse ≤ ×2, 50 produits maximum, jamais « tout le catalogue » ; aperçu périmé → rien n'est appliqué ; annulation qui n'écrase pas une modification ultérieure | `limitsAreEnforcedByTheCode`, `hundredPercentPromotionAndPromotionWithoutEndAreRefused`, `stalePreviewChangesNothing`, `databaseRefusesAPromotionWithoutEndEvenIfTheCodeWereBypassed` |
| LLM07 Fuite du prompt système | Le prompt ne contient aucun secret ni règle de sécurité dont dépendrait la protection : le lire n'apprend rien d'exploitable | Relecture (`agent/*-system-prompt.txt`) |
| LLM08 Faiblesses des vecteurs | Seuls les textes écrits par l'admin (nom, marque, catégorie, description) sont indexés, jamais les avis ; prix et stock relus en base après la recherche ; produit trouvé « par le sens » jamais choisi d'office | `similarProductsAreReadAgainFromDatabase`, `productFoundOnlyBySenseIsNeverPickedAutomatically` |
| LLM09 Désinformation | Prompt : tout chiffre vient d'un outil ; avertissement si un montant ne correspond à rien ; « aucun prix n'a été modifié » si seul un aperçu a été calculé ; l'interface rappelle que les fiches et le bouton « Confirmer » font foi | `previewAloneIsNeverPresentedAsAChange`, évaluation (`./mvnw test -Peval`) |
| LLM10 Consommation non bornée | 20 messages/min par utilisateur (429) ; 1 000 caractères par message ; 6 appels d'outils par tour, 3 par outil ; mémoire limitée aux 20 derniers messages ; journal `agent_tool_calls` (outil, arguments, résultat, durée) | `tooManyMessagesAreRejected`, `runawayToolLoopIsStopped` |
| A10 (SSRF) | Aucun outil ne fait d'appel réseau choisi par le modèle : seul Ollama, à une adresse fixée par la configuration | Relecture |

Risques restants :

| # | Gravité | Constat | Traitement | État |
|---|---|---|---|---|
| 12 | Moyenne | Le petit modèle local (3 milliards de paramètres) se trompe : date mal choisie, « c'est fait » alors que rien n'est fait, outil oublié | Sans conséquence grâce à la confirmation (la carte dit la vérité) ; mesuré par l'évaluation ; modèle plus capable en production (changement de configuration Spring AI) | Accepté, mesuré |
| 13 | Moyenne | Limite de débit de l'assistant en mémoire, par instance : contournable avec plusieurs instances | Stockage partagé (Redis) si l'API passe à plusieurs instances | Planifié (étape 9) |
| 14 | Faible | Modèles téléchargés depuis le registre Ollama sans vérification d'empreinte (chaîne d'approvisionnement, LLM03) | Épingler l'empreinte (digest) du modèle dans le déploiement | Planifié (étape 9) |
| 15 | Faible | Une réponse lente (≈ 1 min sur processeur) occupe une connexion HTTP ; beaucoup d'utilisateurs simultanés satureraient le serveur | File d'attente / délai maximal par réponse, modèle sur GPU ou hébergé | Planifié (étape 9) |

