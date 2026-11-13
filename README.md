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

## Déploiement sur Render et Vercel

Render héberge l'API Spring Boot et PostgreSQL; Vercel héberge le frontend et relaie `/api/*` vers l'API. Cette configuration garde les cookies de session sur le domaine du frontend.

1. Sur Render, créer un **Blueprint** à partir du dépôt GitHub et confirmer la création de `erfolg-zentrum-api` et `erfolg-zentrum-db` définis dans `render.yaml`. Le Blueprint génère `ADMIN_PASSWORD`; conserve-le depuis les variables d'environnement du service.
2. Sur Vercel, importer le même dépôt et sélectionner `frontend` comme **Root Directory**. Garder Vite et `dist` comme répertoire de sortie.
3. Dans les variables du projet Vercel, ajouter `BACKEND_URL=https://erfolg-zentrum-api.onrender.com`, puis redéployer. Si Render attribue une URL différente, utiliser cette URL à la place.
4. Render Free peut mettre le service en veille; le premier appel après une période inactive peut être lent. La base PostgreSQL `basic-256mb` est un plan payant; vérifier le tarif affiché par Render avant de confirmer la création.

Ajouter les variables SMTP sur Render si les courriels doivent être envoyés. Ne jamais publier de mot de passe ni de chaîne de connexion contenant des secrets.