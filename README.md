# Erfolg Zentrum

Application de gestion des inscriptions aux cours d'allemand, avec espace d'administration, génération de fiches PDF et notifications par e-mail.

## Prérequis

- Java 25
- Docker Compose uniquement pour le mode PostgreSQL

Le wrapper Maven télécharge Maven et la configuration du projet construit le frontend pendant le build.

## Démarrage local avec H2

Dans PowerShell, depuis le dossier du projet:

```powershell
$env:ADMIN_PASSWORD = "choisir-un-mot-de-passe-long"
.\mvnw.cmd spring-boot:run
```

Ouvrir `http://localhost:8080/` pour le formulaire d'inscription et `http://localhost:8080/admin.html` pour l'administration. Le nom d'utilisateur administrateur vaut `admin` par défaut et peut être changé avec `ADMIN_USERNAME`. Le mot de passe `ADMIN_PASSWORD` est obligatoire.

H2 conserve ses données localement dans `data/`; ces fichiers ne doivent pas être publiés.
La capacité par groupe (formation + niveau + centre) est de 20 places par défaut; elle peut être ajustée avec `APP_ENROLLMENT_CAPACITY`. Les demandes au-delà de cette limite sont enregistrées sur la liste d’attente.
Les inscriptions peuvent aussi s’ouvrir automatiquement à partir d’une date de référence grâce à `APP_ENROLLMENT_OPENS_ON` et `APP_ENROLLMENT_OPENING_MARGIN_DAYS` (25 par défaut). Si une date est renseignée, la plateforme s’ouvre automatiquement 25 jours avant cette date et bloque l’enregistrement avant ce moment ; la liste d’attente reste active si le groupe est complet.

## Démarrage avec PostgreSQL

Définir les variables avant de démarrer la base et l'application:

```powershell
$env:DATABASE_PASSWORD = "choisir-un-mot-de-passe-de-base"
$env:ADMIN_PASSWORD = "choisir-un-mot-de-passe-administrateur"
docker compose up -d
$env:SPRING_PROFILES_ACTIVE = "postgres"
.\mvnw.cmd spring-boot:run
```

Les valeurs par défaut de la base sont `erfolg` pour le nom de la base et l'utilisateur. `DATABASE_USERNAME` et `DATABASE_URL` permettent de les modifier. Le volume Docker conserve les données après l'arrêt des conteneurs; `docker compose down` n'efface pas ce volume.

## E-mails

L'envoi est désactivé tant que le serveur SMTP n'est pas configuré. Définir les variables Spring Boot `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` et `SPRING_MAIL_PASSWORD`, ainsi que `MAIL_FROM` si l'adresse expéditrice doit changer. Les secrets doivent rester dans l'environnement ou dans un gestionnaire de secrets, jamais dans le dépôt.

## Vérification et package

```powershell
.\mvnw.cmd clean verify
```

La commande exécute les tests backend et construit le frontend. Le JAR Spring Boot est produit dans `target/`.

Pour reconstruire uniquement le frontend:

```powershell
Push-Location frontend
npm ci
npm run build
Pop-Location
```

## Déploiement sur Vercel

Vercel héberge le frontend React; l'API Spring Boot doit être hébergée séparément sur un service compatible avec les conteneurs et reliée à une base PostgreSQL persistante.

1. Importer le dépôt GitHub dans Vercel. Dans **Root Directory**, choisir `frontend`; utiliser le preset **Vite** et le répertoire de sortie `dist`.
2. Après avoir déployé l'API, ajouter la variable Vercel `VITE_API_BASE_URL` avec son origine publique, par exemple `https://api.example.com` (sans `/` final), puis redéployer le frontend.
3. Sur l'hébergement de l'API, définir `DATABASE_URL` (format JDBC, par exemple `jdbc:postgresql://hote:5432/base?sslmode=require`), `DATABASE_USERNAME`, `DATABASE_PASSWORD` et `ADMIN_PASSWORD`. Définir aussi `APP_CORS_ALLOWED_ORIGIN` à l'origine exacte du frontend Vercel, `SESSION_COOKIE_SECURE=true` et `CSRF_COOKIE_SECURE=true`. La capacité s’ajuste avec `APP_ENROLLMENT_CAPACITY` (20 par défaut).
4. Pour éviter le blocage des cookies de session par les navigateurs, utiliser un domaine personnalisé commun pour le frontend et l'API (par exemple `www.example.com` et `api.example.com`). Ajouter les variables SMTP décrites ci-dessus si les courriels doivent être envoyés.

Avant que l'API soit déployée et que `VITE_API_BASE_URL` soit renseignée, le site statique se construit mais le formulaire et l'administration ne peuvent pas accéder aux données. Ne pas mettre de mot de passe ni d'URL contenant un secret dans le dépôt.