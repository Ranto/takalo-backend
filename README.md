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
3. Créer **un seul client** pour le frontend Angular (le backend n'en a pas besoin — voir encadré ci-dessous) :
   - Client ID : `takalo-frontend`
   - Client type : **OpenID Connect**
   - Client authentication : **Off** (client public, SPA — pas de secret côté navigateur)
   - Standard flow : **On** (Authorization Code + PKCE)
   - Direct access grants : **Off** (à activer uniquement pour tester en password grant via `curl`, cf. §2.6)
   - Valid redirect URIs :
     - `http://localhost:4200/auth/callback`
     - `http://localhost:4200/silent-refresh.html`
   - Valid post logout redirect URIs : `http://localhost:4200/`
   - Web origins : `http://localhost:4200` (ou `+` pour reprendre les redirect URIs)

   > ℹ️ **Pourquoi pas de client `takalo-backend` ?** Le backend Spring est un *Resource Server* OAuth2 : il ne fait que **valider** les JWT reçus en `Authorization: Bearer …` (via les clés publiques téléchargées depuis `KEYCLOAK_ISSUER_URI`). Il n'initie aucun flux de login et ne s'enregistre donc pas comme client Keycloak. Un seul client (`takalo-frontend`) suffit pour toute l'application.
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

### 2.6 Obtenir un access token (test CLI)

Pratique pour tester l'API avec `curl` / Postman / Swagger UI sans passer par le navigateur. Pré-requis : activer **Direct access grants** sur le client `takalo-frontend` dans Keycloak.

```bash
curl -X POST "http://localhost:8081/realms/takalo/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=takalo-frontend" \
  -d "username=<user>" \
  -d "password=<password>"
```

Utiliser le `access_token` dans l'en-tête `Authorization: Bearer <token>` ou via le bouton **Authorize** de Swagger UI.

> ⚠️ Le grant `password` est déconseillé en production (OAuth 2.1) ; il n'est utile qu'en dev. Le frontend, lui, utilise systématiquement Authorization Code + PKCE.

### 2.7 Thème de login Takalo

Un thème Keycloak personnalisé (couleurs et logo Takalo) est fourni dans `keycloak-themes/takalo/`. Il est monté automatiquement par `docker-compose.yml` sur `/opt/keycloak/themes` ; le cache de thèmes est désactivé en mode `start-dev` pour rafraîchir les modifications sans redémarrer le conteneur.

Activer le thème (à faire une seule fois) :

1. http://localhost:8081 → admin → realm `takalo` → **Realm Settings** → onglet **Themes**.
2. **Login theme** = `takalo` → **Save**.

Structure du thème :

```
keycloak-themes/takalo/login/
├── theme.properties              # hérite du thème keycloak, charge takalo.css
└── resources/
    ├── css/takalo.css            # palette Takalo (navy + jaune), inputs, boutons
    └── img/logo.svg              # logo affiché dans le header
```

Personnalisations courantes :

- **Logo** : remplacer `resources/img/logo.svg` (PNG/SVG, ~200×56) — pas de redémarrage nécessaire en dev.
- **Styles** : éditer `resources/css/takalo.css`. Recharger la page de login pour voir les changements.
- **Textes (i18n)** : créer `login/messages/messages_fr.properties`, par ex. `loginAccountTitle=Connexion à Takalo`.
- **Templates HTML** : copier les `*.ftl` souhaités depuis le thème de base (`docker exec takalo-keycloak find /opt/keycloak/lib -name 'login.ftl'`) vers `keycloak-themes/takalo/login/` puis adapter.

> ⚠️ **Prod** : retirer les flags `--spi-theme-cache-*=false` du `docker-compose.yml` (ou passer Keycloak en `start` au lieu de `start-dev`) pour réactiver le cache et bénéficier des perfs.

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

#### Sémantique `:own` vs `:any` sur les achats

Chaque achat porte un `owner_id` (l'utilisateur à qui il appartient — distinct des champs d'audit `created_by`/`modified_by`). Sur les endpoints `GET /api/v1/purchases/**`, la règle s'applique au niveau service :

- `PERM_purchase:read:any` → accès à tous les achats.
- `PERM_purchase:read:own` (sans `:any`) → ne voit que les achats dont `owner_id` correspond à l'utilisateur courant. La recherche paginée filtre automatiquement par owner ; un `GET /{id}` sur un achat tiers répond **403**.

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

## 8. Déploiement Docker via GHCR + Caddy

Approche alternative à §7 : le backend est packagé en image Docker, publiée sur **GitHub Container Registry (GHCR)**, puis tirée sur un VPS où **Caddy** (installé nativement) sert de reverse proxy TLS pour plusieurs applications.

Fichiers fournis à la racine du backend :
- `Dockerfile` — build multi-stage Maven → JRE 21
- `docker-compose.prod.yml` — stack serveur (postgres + keycloak + app), ports en `127.0.0.1` uniquement
- `.env.prod.example` — template à copier en `.env` sur le serveur
- `.dockerignore`

### 8.1 Pré-requis VPS

- Docker 24+ et Docker Compose v2 (`docker compose`)
- Caddy déployé en conteneur Docker (sa propre stack `compose`, à côté de la stack Takalo). Caddy rejoint le réseau `takalo-network` en *external* pour atteindre `app` et `keycloak` via le DNS Docker. Détails au §8.4.
- Sous-domaines `takalo.ranto.ovh`, `api.takalo.ranto.ovh` et `auth.takalo.ranto.ovh` pointés sur l'IP du VPS (DNS OVH)

### 8.2 Créer le GitHub Container Registry (une seule fois)

GHCR est l'équivalent GitHub du Container Registry GitLab. Pas de "création" explicite : il suffit de pousser une image taggée `ghcr.io/<user>/<image>:<tag>`, et le package apparaît sous l'onglet **Packages** du compte GitHub.

1. **Créer un Personal Access Token (classic)** sur GitHub :
   - `Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token (classic)`
   - Nom : `ghcr-takalo`, expiration au choix
   - Cocher **`write:packages`** (inclut automatiquement `read:packages`)
   - Copier le token (visible une seule fois)

2. **Login Docker en local** (machine de build) :

   ```bash
   echo "<TOKEN>" | docker login ghcr.io -u <ton-user-github> --password-stdin
   ```

3. **Premier build + push** depuis le dossier

   ```bash
   docker build -t ghcr.io/<ton-user-github>/takalo-backend:latest .
   docker push ghcr.io/<ton-user-github>/takalo-backend:latest
   ```

4. **Choisir la visibilité du package** (privé par défaut) : `https://github.com/<user>?tab=packages → takalo-backend → Package settings → Change visibility`.
   - **Public** : plus simple, pas besoin de login sur le serveur.
   - **Privé** : plus sûr, nécessite `docker login ghcr.io` sur le VPS.

5. **(Si privé) Login Docker sur le VPS** :

   ```bash
   echo "<TOKEN>" | docker login ghcr.io -u <ton-user-github> --password-stdin
   ```

> 💡 **Versioning** : tagger chaque release avec une version en plus de `latest` (`:1.0.0`, `:$(git rev-parse --short HEAD)`) pour pouvoir rollback en changeant simplement `APP_VERSION` dans le `.env` du serveur.

### 8.3 Préparer le serveur (une seule fois)

```bash
mkdir -p /opt/takalo && cd /opt/takalo
# Copier docker-compose.prod.yml et .env.prod.example via scp ou git
cp .env.prod.example .env
nano .env   # remplir GITHUB_USER, DB_PASSWORD, KEYCLOAK_ADMIN_PASSWORD, …
```

Variables du `.env` (toutes obligatoires) :

| Variable                                            | Description                                                              |
| --------------------------------------------------- | ------------------------------------------------------------------------ |
| `GITHUB_USER`                                       | propriétaire de l'image GHCR                                             |
| `APP_VERSION`                                       | tag de l'image (`latest` ou version semver)                              |
| `DB_USER` / `DB_PASSWORD`                           | credentials PostgreSQL (base `takalo_db`)                                |
| `KEYCLOAK_ADMIN_USER` / `KEYCLOAK_ADMIN_PASSWORD`   | bootstrap admin de Keycloak                                              |
| `KEYCLOAK_DB_USER` / `KEYCLOAK_DB_PASSWORD`         | credentials PostgreSQL pour la base `keycloak_db` (utilisateur dédié)    |

### 8.4 Configurer Caddy (Docker)

Caddy tourne dans son propre conteneur, géré par une stack `compose` séparée (typiquement `/opt/caddy/`), pour pouvoir mutualiser un seul reverse proxy entre Takalo et d'autres applications du VPS.

**`/opt/caddy/docker-compose.yml`** (extrait minimal) :

```yaml
services:
  caddy:
    image: caddy:2
    container_name: caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
      - "443:443/udp"   # HTTP/3
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy-data:/data
      - caddy-config:/config
    networks:
      - takalo-network

volumes:
  caddy-data:
  caddy-config:

networks:
  takalo-network:
    external: true   # créé par la stack Takalo (docker-compose.prod.yml)
```

**`/opt/caddy/Caddyfile`** :

```caddy
api.takalo.ranto.ovh {
    reverse_proxy app:8080
}

auth.takalo.ranto.ovh {
    reverse_proxy keycloak:8080
}
```

> ℹ️ Comme Caddy partage le réseau `takalo-network` avec les autres services, il résout `app` et `keycloak` via le DNS Docker (noms de service) — plus besoin d'exposer leurs ports sur `127.0.0.1` côté hôte. Les bindings `127.0.0.1:8080` / `127.0.0.1:8088` du `docker-compose.prod.yml` ne sont alors utiles que pour du debug local (`curl 127.0.0.1:8080/actuator/health`) et peuvent être retirés.

Démarrer / recharger :

```bash
# Premier démarrage
cd /opt/caddy
docker compose up -d

# Recharger après modification du Caddyfile (sans downtime)
docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile

# Valider la syntaxe avant rechargement
docker compose exec caddy caddy validate --config /etc/caddy/Caddyfile
```

Caddy provisionne automatiquement les certificats Let's Encrypt et les stocke dans le volume `caddy-data` (à sauvegarder). Vérifier d'abord que le DNS résout vers l'IP du VPS, sinon la provision TLS échouera.

### 8.5 Premier démarrage

Keycloak utilise PostgreSQL (base `keycloak_db`, user dédié `KEYCLOAK_DB_USER`) — pas H2. Il faut créer la base et le user **avant** le premier `up -d`, sinon Keycloak ne démarrera pas. Démarrer d'abord uniquement Postgres :

```bash
cd /opt/takalo
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d postgres

# Créer le user et la base Keycloak (remplacer les valeurs par celles du .env)
docker exec -i takalo-db psql -U "$DB_USER" -d takalo_db <<SQL
CREATE USER ${KEYCLOAK_DB_USER} WITH PASSWORD '${KEYCLOAK_DB_PASSWORD}';
CREATE DATABASE keycloak_db OWNER ${KEYCLOAK_DB_USER};
SQL
```

Puis lancer le reste de la stack :

```bash
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml logs -f app
```

Au premier boot, l'app **échouera** : le realm `takalo` n'existe pas encore dans Keycloak, donc l'`issuer-uri` est introuvable. C'est attendu — faire le setup Keycloak ci-dessous puis redémarrer l'app.

### 8.6 Setup Keycloak (nouvelle instance)

Ouvrir `https://auth.takalo.ranto.ovh` (login : credentials admin du `.env`), puis suivre la procédure §2.3 (création du realm `takalo`, du client `takalo-frontend`, …) — adapter les redirect URIs au domaine de prod :

- Valid redirect URIs : `https://takalo.ranto.ovh/auth/callback`, `https://takalo.ranto.ovh/silent-refresh.html`
- Valid post logout redirect URIs : `https://takalo.ranto.ovh/`
- Web origins : `https://takalo.ranto.ovh`

Puis redémarrer l'app :

```bash
docker compose -f docker-compose.prod.yml restart app
```

Bootstrap du premier admin : cf. §2.5 — en prod, exécuter le `INSERT` via :

```bash
docker exec -it takalo-db psql -U <DB_USER> -d takalo_db
```

### 8.7 Workflow de mise à jour (déploiements suivants)

En local après chaque release :

```bash
docker build -t ghcr.io/<user>/takalo-backend:latest .
docker push ghcr.io/<user>/takalo-backend:latest
```

Sur le serveur :

```bash
cd /opt/takalo
docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d app
```

Caddy reste intouché. Postgres et Keycloak ne sont rebuildés que si tu changes leur version d'image dans `docker-compose.prod.yml`.

### 8.9 Headers de sécurité Caddy

Caddy peut ajouter en bordure une couche de headers HTTP de sécurité (HSTS, CSP, Permissions-Policy, …) pour durcir l'application sans toucher au backend. Comme la stack expose **trois domaines distincts** (front, API, Keycloak) qui n'ont pas les mêmes contraintes, on définit trois snippets.

| Domaine                   | Ce qu'il sert         | Politique                                                                                |
| ------------------------- | --------------------- | ---------------------------------------------------------------------------------------- |
| `takalo.ranto.ovh`        | Angular SPA (HTML/JS) | **CSP complète** — c'est là que les XSS sont une menace réelle                           |
| `api.takalo.ranto.ovh`    | JSON Spring Boot      | Headers de base seulement (HSTS, nosniff, COOP/CORP). Pas besoin de CSP stricte          |
| `auth.takalo.ranto.ovh`   | UI Keycloak           | **Ne pas imposer de CSP depuis Caddy** — Keycloak gère sa propre CSP par realm           |

> ⚠️ Coller la CSP du frontend sur `auth.takalo.ranto.ovh` casse les pages de login Keycloak (templates Freemarker avec scripts/styles inline). Sur l'API, une CSP stricte est inutile : CSP s'applique au rendu HTML par le navigateur, pas aux réponses JSON consommées par `fetch`.

À ajouter dans `/opt/caddy/Caddyfile` (le fichier monté en volume read-only sur le conteneur Caddy, cf. §8.4) :

```caddy
# ----- Snippets réutilisables -----

(security_base) {
    header {
        Strict-Transport-Security "max-age=63072000; includeSubDomains; preload"
        X-Content-Type-Options "nosniff"
        Referrer-Policy "strict-origin-when-cross-origin"
        -Server
    }
}

(security_frontend) {
    import security_base
    header {
        X-Frame-Options "DENY"
        Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self' https://api.takalo.ranto.ovh https://auth.takalo.ranto.ovh; frame-src https://auth.takalo.ranto.ovh; frame-ancestors 'none'; form-action 'self'; base-uri 'self'; object-src 'none'; upgrade-insecure-requests"
        Permissions-Policy "accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=(), interest-cohort=()"
        Cross-Origin-Opener-Policy "same-origin"
        Cross-Origin-Resource-Policy "same-origin"
    }
}

(security_api) {
    import security_base
    header {
        X-Frame-Options "DENY"
        Cross-Origin-Resource-Policy "same-site"
    }
}

# ----- Sites -----

takalo.ranto.ovh {
    import security_frontend
    encode gzip zstd
    root * /srv/takalo-frontend
    try_files {path} /index.html
    file_server
}

api.takalo.ranto.ovh {
    import security_api
    encode gzip zstd
    reverse_proxy app:8080
}

auth.takalo.ranto.ovh {
    import security_base
    encode gzip zstd
    reverse_proxy keycloak:8080
}
```

#### Détail des directives CSP du frontend

- `default-src 'self'` — fallback : tout ce qui n'est pas surchargé doit venir du domaine frontend.
- `script-src 'self'` — pas d'`'unsafe-inline'`, pas d'`'unsafe-eval'`. Angular en build prod n'en a plus besoin. Si une violation apparaît, c'est un script inline qui traîne dans `index.html` — corriger côté code plutôt que d'assouplir la CSP.
- `style-src 'self' 'unsafe-inline'` — Angular injecte des styles inline pour les composants. `'unsafe-inline'` pour les styles est largement moins risqué que pour les scripts (pas d'exécution de code).
- `connect-src 'self' https://api.takalo.ranto.ovh https://auth.takalo.ranto.ovh` — fetch/XHR autorisés vers l'API REST **et** Keycloak (discovery doc, JWKS, token endpoint, userinfo). Sans ces deux origines, `angular-oauth2-oidc` plante au démarrage.
- `frame-src https://auth.takalo.ranto.ovh` — autorise l'iframe utilisée par `silent-refresh.html` pour le renouvellement de token muet.
- `frame-ancestors 'none'` — équivalent moderne et plus précis de `X-Frame-Options: DENY`. Garder les deux ne pose pas de problème.
- `form-action 'self'` — empêche un attaquant XSS d'injecter un `<form action="https://evil/">` qui exfiltre des données saisies.
- `base-uri 'self'` — bloque l'injection d'un `<base>` qui détournerait toutes les URLs relatives.
- `object-src 'none'` — pas de `<object>`/`<embed>`/Flash. Devrait toujours être à `'none'`.
- `upgrade-insecure-requests` — réécrit automatiquement les éventuelles URLs `http://` en `https://` dans la page (filet de sécurité, pas une excuse pour laisser du `http://` dans le code).

#### Pourquoi `same-site` (pas `same-origin`) sur l'API

`Cross-Origin-Resource-Policy: same-origin` rendrait l'API inaccessible depuis le SPA car `takalo.ranto.ovh` et `api.takalo.ranto.ovh` sont des **origines** différentes. Elles sont en revanche sur le même **site** (suffixe public commun), donc `same-site` autorise les requêtes cross-origin du frontend tout en bloquant les sites tiers.

#### CSP côté Keycloak

Pour durcir Keycloak, c'est dans **Realm Settings → Security defenses → Headers** dans la console admin, pas dans Caddy. Par défaut Keycloak envoie déjà `frame-src 'self'; frame-ancestors 'self'; object-src 'none';` — adapter `frame-ancestors` à `https://takalo.ranto.ovh` si tu intègres Keycloak en iframe depuis le frontend (cas du silent refresh).

#### Méthodologie de déploiement

1. Démarrer en mode *report-only* d'abord, pour ne rien casser : remplacer `Content-Security-Policy` par `Content-Security-Policy-Report-Only` dans `(security_frontend)`. Les violations sont loggées dans la console du navigateur sans bloquer l'exécution.
2. Naviguer dans toute l'application pendant 24-48h, vérifier zéro violation.
3. Repasser sur `Content-Security-Policy` (bloquant).
4. (Optionnel) Ajouter un `report-uri` ou `report-to` pour centraliser les violations en prod.

#### Recharger Caddy

```bash
cd /opt/caddy
docker compose exec caddy caddy validate --config /etc/caddy/Caddyfile
docker compose exec caddy caddy reload --config /etc/caddy/Caddyfile
```

> ℹ️ Si tu sers le frontend statique (`takalo.ranto.ovh`) directement par Caddy, monte aussi le dossier des assets en volume read-only dans le conteneur Caddy (`./takalo-frontend:/srv/takalo-frontend:ro` à ajouter dans le compose §8.4).

### 8.10 Pièges classiques

- **Keycloak derrière proxy** — `KC_HOSTNAME` + `KC_PROXY_HEADERS=xforwarded` sont obligatoires (déjà présents dans `docker-compose.prod.yml`). L'`issuer-uri` envoyé dans les JWT doit **exactement** matcher l'URL publique, sinon le backend rejette les tokens.
- **`start-dev` Keycloak** — non officiellement supporté en prod (pas optimisé). Pour un usage perso ça passe ; pour du sérieux, basculer sur `start --optimized` après un build préalable.
- **Postgres non exposé à Internet** — vérifier `docker ps` : le mapping doit afficher `5432/tcp` sans `0.0.0.0:`. Idem keycloak/app : `127.0.0.1:8080->8080/tcp`.
- **Migrations Flyway** — `baseline-on-migrate=false` en prod : Flyway refuse de démarrer si la DB existe avec un schéma divergent.

---

## 9. Dépannage

| Symptôme                                                            | Piste                                                                                          |
| ------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| `ddl-auto=validate` échoue au démarrage                             | Schéma JPA et SQL désynchronisés → vérifier qu'une migration est manquante/incorrecte.         |
| 401 sur tous les endpoints                                          | Token absent/expiré, ou `KEYCLOAK_ISSUER_URI` incorrect (Keycloak doit être joignable au boot).|
| 403 alors que le user est ADMIN                                     | Le rôle a été assigné après la génération du token → re-générer un token (re-login).           |
| `relation "users" does not exist`                                   | Flyway n'a pas tourné — vérifier `spring.flyway.enabled=true` et l'accès DB.                   |
| Keycloak inaccessible depuis l'app en prod (TLS)                    | L'`issuer-uri` doit être joignable depuis le pod/serveur app. Vérifier DNS, certificats, proxy.|

---

## 10. Liens utiles

- Swagger UI : http://localhost:8080/swagger-ui.html
- OpenAPI JSON : http://localhost:8080/v3/api-docs
- Keycloak admin (dev) : http://localhost:8081
- Documentation Spring Security OAuth2 Resource Server : https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
