# Takalo — Backend

API REST de l'application Takalo : gestion des catégories, produits, achats et utilisateurs (RBAC).

- **Stack** : Spring Boot 4.0.5, Java 21, Maven, PostgreSQL 16, Flyway, Keycloak (OAuth2)
- **Architecture** : hexagonale (ports & adapters), organisée par bounded context (`category`, `product`, `purchase`, `user`, `shared`)
- **Documentation API** : Swagger UI sur `/swagger-ui.html`

---

## 1. Prérequis

| Outil          | Version        | Notes                                 |
| -------------- | -------------- | ------------------------------------- |
| JDK            | 21             | Temurin / Adoptium recommandé         |
| Maven Wrapper  | inclus         | `./mvnw` (Linux/macOS), `mvnw.cmd` (Windows) |
| Docker         | 24+            | Avec Docker Compose v2 (`docker compose`) |
| Git            | 2.40+          |                                       |

Pas besoin d'installer Maven globalement : le projet utilise le wrapper.

---

## 2. Installation locale (développement)

### 2.1 Cloner le projet

```bash
git clone <url-du-repo> takalo
cd takalo/backend
```

### 2.2 Démarrer PostgreSQL + Keycloak

Le `docker-compose.yml` fournit les deux services nécessaires :

```bash
docker compose up -d
```

- PostgreSQL : `localhost:5432`, db `takalo_db`, user `admin` / `admin123`
- Keycloak : http://localhost:8081 (admin console), bootstrap admin `admin` / `admin`

### 2.3 Configurer Keycloak (réalisé une seule fois)

1. Aller sur http://localhost:8081 → se connecter avec `admin` / `admin`.
2. Créer un realm `takalo` (menu déroulant en haut à gauche → **Create realm**).
3. Créer un client :
   - Client ID : `takalo-backend`
   - Client type : **OpenID Connect**
   - Client authentication : **Off** (client public, pour test local)
   - Valid redirect URIs : `http://localhost:*` (à restreindre en prod)
   - Web origins : `*` (à restreindre en prod)
4. (Optionnel) Activer l'auto-inscription par email + mot de passe :
   - **Realm Settings** → onglet **Login** → activer **User registration** (et **Forgot password**, **Remember me** si souhaité).
   - Un lien *Register* apparaîtra alors sur la page de login Keycloak.
5. Créer un utilisateur de test :
   - Onglet **Users** → **Add user** → renseigner `username`, `email` (cocher *Email verified*).
   - Onglet **Credentials** → définir un mot de passe (décocher *Temporary*).

### 2.4 Lancer l'application

```bash
./mvnw spring-boot:run
```

L'app écoute sur http://localhost:8080. Profil Spring actif par défaut : `dev`.

DevTools est sur le classpath runtime → hot reload actif si vous lancez via votre IDE.

### 2.5 Bootstrap du premier admin

Au premier appel authentifié, l'utilisateur est provisionné automatiquement avec le rôle `USER`. Pour disposer d'un administrateur, exécuter une fois :

```sql
-- Connexion : psql -h localhost -U admin -d takalo_db
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@example.com'   -- email du user Keycloak
  AND r.name = 'ADMIN';
```

(Une fois ce premier admin posé, les suivants peuvent être gérés via `POST /api/v1/users/{id}/roles`.)

### 2.6 Obtenir un access token

```bash
curl -X POST "http://localhost:8081/realms/takalo/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=takalo-backend" \
  -d "username=<user>" \
  -d "password=<password>"
```

Utiliser le `access_token` dans l'en-tête `Authorization: Bearer <token>` ou via le bouton **Authorize** de Swagger UI.

---

## 3. Commandes utiles

```bash
# Compiler
./mvnw clean compile

# Lancer les tests
./mvnw test

# Lancer un test précis
./mvnw test -Dtest=TakaloApplicationTests
./mvnw test -Dtest=ProductCategoryServiceTest#shouldCreate

# Builder un jar exécutable
./mvnw clean package          # génère target/takalo-0.0.1-SNAPSHOT.jar

# Vérifier le statut des migrations Flyway
./mvnw flyway:info
```

---

## 4. Structure du projet

```
src/main/java/ara/project/takalo/
├── category/                   # bounded context Catégories
├── product/                    # bounded context Produits
├── purchase/                   # bounded context Achats (+ import Excel)
├── user/                       # bounded context Utilisateurs + RBAC
└── shared/                     # exceptions, pagination, OpenAPI, GlobalExceptionHandler
```

Chaque bounded context suit la même structure :

```
<context>/
├── application/
│   ├── port/in/                # interfaces use-case (ServicePort)
│   ├── port/out/               # interfaces repository (Repository)
│   └── service/                # implémentations @Service @Transactional
├── domain/
│   └── model/                  # records immuables
└── infrastructure/
    ├── persistence/            # JPA repositories, entities, adapters, mappers
    └── rest/                   # controllers, DTOs, web mappers
```

**Invariant clé** : la couche `domain/` n'importe ni JPA, ni Spring Data, ni les types web.

Migrations SQL : `src/main/resources/db/migration/V<n>__<name>.sql`. Toujours créer une nouvelle migration, jamais modifier une migration appliquée (`ddl-auto=validate`).

---

## 5. Sécurité & RBAC

L'API est **stateless** et protégée par OAuth2 Resource Server. Les JWT sont validés contre l'`issuer-uri` Keycloak. Au premier appel authentifié, un `User` Takalo est provisionné en DB (JIT provisioning) avec le rôle par défaut `USER`.

### Rôles seedés (`V7__create_users_rbac.sql`)

| Rôle    | Permissions                                                                                                     |
| ------- | ---------------------------------------------------------------------------------------------------------------- |
| `ADMIN` | toutes                                                                                                          |
| `USER`  | `category:read`, `product:read`, `purchase:create`, `purchase:read:own`                                         |

### Permissions

`category:manage`, `category:read`, `product:write`, `product:read`, `purchase:create`, `purchase:read:own`, `purchase:read:any`, `purchase:import`, `user:manage`.

Dans le code, exprimer les contraintes par **permission** plutôt que par rôle :

```java
@PreAuthorize("hasAuthority('PERM_product:write')")
```

Les rôles sont également exposés en `ROLE_<name>` pour les cas où c'est plus parlant.

### Endpoints publics

`/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**`, `/actuator/health`, `/actuator/info`. Tout le reste exige une authentification.

### Connexion par email + mot de passe

C'est le mode natif de Keycloak — **rien à coder côté Takalo**. Les mots de passe sont stockés et hashés par Keycloak (politique de force, lockout, reset par email, MFA, audit fournis nativement).

Activation :

1. Dans Keycloak : **Realm Settings → Login → User registration = On** (cf. §2.3 étape 4) pour permettre aux utilisateurs de s'inscrire eux-mêmes. Sinon, créer les comptes manuellement dans **Users**.
2. Côté frontend, intégrer une librairie OIDC (`keycloak-js`, `oidc-client-ts`, `@auth0/auth0-spa-js` compatible OIDC, …) en **Authorization Code + PKCE**. La page de login Keycloak affichera alors *email + password*, et — si Google est configuré — un bouton **Se connecter avec Google** sur le même écran.

> ⚠️ Le flux **Resource Owner Password Credentials** (montré au §2.6 pour récupérer un token en CLI) est pratique pour tester avec `curl` / Postman, mais déconseillé en production par la spec OAuth 2.1 : préférer Authorization Code + PKCE depuis le frontend.

Politique de mot de passe (longueur, complexité, expiration, historique) : **Authentication → Policies → Password policy** dans Keycloak.

### Connexion via Google (Identity Brokering)

Keycloak peut déléguer l'authentification à Google (ou autre IdP OIDC : Microsoft, GitHub, …). **Aucun changement côté backend Takalo** : Keycloak reste l'unique émetteur des JWT, et le `JwtAuthConverter` provisionne le `User` Takalo en JIT comme pour un login mot de passe.

Configuration (à faire une fois dans Keycloak) :

1. **Créer des credentials OAuth dans Google Cloud Console** (https://console.cloud.google.com/) :
   - Type : *OAuth client ID* → *Web application*.
   - Authorized redirect URI : `http://localhost:8081/realms/takalo/broker/google/endpoint` (dev) ou `https://auth.example.com/realms/takalo/broker/google/endpoint` (prod).
   - Récupérer `Client ID` + `Client Secret`.

2. **Dans Keycloak**, realm `takalo` → **Identity Providers** → **Add provider** → **Google** → coller les credentials.

3. Laisser **First Login Flow** sur `first broker login` (par défaut). Au premier login Google, Keycloak crée l'utilisateur dans son realm à partir de `email` + `name` retournés par Google.

Points de vigilance :

- **`sub` du JWT** = ID Keycloak (pas ID Google) → `users.external_id` reste stable.
- **Conflit d'emails** : si un user `foo@gmail.com` existe déjà en password-based dans Keycloak, un *account linking flow* se déclenche au premier login Google (paramétrable dans le First Login Flow).
- **`email_verified`** : Google le renvoie à `true`. Pour forcer ce check côté Takalo, ajouter une règle dans `JwtAuthConverter` (rejeter le provisioning si false).

---

## 6. Variables d'environnement

| Variable                | Profil    | Défaut (dev)                                          | Description                              |
| ----------------------- | --------- | ----------------------------------------------------- | ---------------------------------------- |
| `SPRING_PROFILES_ACTIVE`| tous      | `dev`                                                 | `dev` ou `prod`                          |
| `DB_URL`                | tous      | `jdbc:postgresql://localhost:5432/takalo_db`          | URL JDBC PostgreSQL                      |
| `DB_USER`               | tous      | `admin`                                               | Utilisateur DB                           |
| `DB_PASSWORD`           | tous      | `admin123`                                            | Mot de passe DB                          |
| `KEYCLOAK_ISSUER_URI`   | tous      | `http://localhost:8081/realms/takalo`                 | URI de l'issuer JWT (obligatoire en prod)|

En profil `prod`, **aucune valeur par défaut** : l'application refuse de démarrer si l'une de ces variables manque.

---

## 7. Déploiement en production

### 7.1 Build du livrable

```bash
./mvnw clean package -DskipTests=false
# → target/takalo-0.0.1-SNAPSHOT.jar
```

### 7.2 Pré-requis serveur

- JDK 21 installé (`java -version`).
- Une instance PostgreSQL 16 accessible (managée ou self-hosted), avec une base dédiée et un user applicatif (privilèges sur la base uniquement, pas SUPERUSER).
- Une instance Keycloak (ou autre IdP OIDC) accessible publiquement, avec :
  - un realm dédié (ex. `takalo`) ;
  - un client `takalo-backend` avec **Client authentication : On** en prod (client confidentiel) ;
  - HTTPS obligatoire — l'`issuer-uri` doit être en `https://`.
- Un reverse proxy (Nginx, Traefik) terminant TLS devant l'application.

### 7.3 Lancement

Exporter les variables d'environnement, puis :

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://db.internal:5432/takalo_prod
export DB_USER=takalo_app
export DB_PASSWORD=<secret>
export KEYCLOAK_ISSUER_URI=https://auth.example.com/realms/takalo

java -jar takalo-0.0.1-SNAPSHOT.jar
```

Sur la première exécution, Flyway applique automatiquement toutes les migrations `V1`…`V7`.

### 7.4 Service systemd (exemple)

`/etc/systemd/system/takalo.service` :

```ini
[Unit]
Description=Takalo Backend
After=network.target

[Service]
User=takalo
WorkingDirectory=/opt/takalo
EnvironmentFile=/etc/takalo/takalo.env
ExecStart=/usr/bin/java -jar /opt/takalo/takalo.jar
Restart=on-failure
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

`/etc/takalo/takalo.env` (mode `0600`, propriétaire `takalo`) :

```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://db.internal:5432/takalo_prod
DB_USER=takalo_app
DB_PASSWORD=...
KEYCLOAK_ISSUER_URI=https://auth.example.com/realms/takalo
```

Activer & démarrer :

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now takalo
sudo journalctl -u takalo -f
```

### 7.5 Health checks

- Liveness/readiness : `GET /actuator/health` (publique).
- Configurer le reverse proxy / load balancer pour interroger cet endpoint.

### 7.6 Bootstrap du premier admin (prod)

Comme en dev (cf. §2.5) : se connecter en SQL et insérer une ligne dans `user_roles` pour donner `ADMIN` au premier utilisateur. Ensuite, la gestion se fait via l'API (`POST /api/v1/users/{id}/roles`).

### 7.7 Sauvegardes & migrations

- **DB** : `pg_dump` quotidien minimum, rétention selon politique. Tester la restauration.
- **Schéma** : ne jamais éditer une migration déjà appliquée. Toujours créer un `V<n+1>__<name>.sql`.
- En prod, `spring.flyway.baseline-on-migrate=false` — un état Flyway divergent fera échouer le démarrage volontairement.

---

## 8. Dépannage

| Symptôme                                                            | Piste                                                                                          |
| ------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| `ddl-auto=validate` échoue au démarrage                             | Schéma JPA et SQL désynchronisés → vérifier qu'une migration est manquante/incorrecte.         |
| 401 sur tous les endpoints                                          | Token absent/expiré, ou `KEYCLOAK_ISSUER_URI` incorrect (Keycloak doit être joignable au boot).|
| 403 alors que le user est ADMIN                                     | Le rôle a été assigné après la génération du token → re-générer un token (re-login).           |
| `relation "users" does not exist`                                   | Flyway n'a pas tourné — vérifier `spring.flyway.enabled=true` et l'accès DB.                   |
| Keycloak inaccessible depuis l'app en prod (TLS)                    | L'`issuer-uri` doit être joignable depuis le pod/serveur app. Vérifier DNS, certificats, proxy.|

---

## 9. Liens utiles

- Swagger UI : http://localhost:8080/swagger-ui.html
- OpenAPI JSON : http://localhost:8080/v3/api-docs
- Keycloak admin (dev) : http://localhost:8081
- Documentation Spring Security OAuth2 Resource Server : https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
