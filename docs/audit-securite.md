# Audit de sécurité – OWASP Top 10 (étape 4, revu à l'étape 7)

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

