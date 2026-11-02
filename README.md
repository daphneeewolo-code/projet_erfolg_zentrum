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