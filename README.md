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

## Socle des comptes et des rôles

Les rôles persistants du cahier des charges sont `ADMIN`, `SECRETARY`, `TEACHER`, `STUDENT` et `PARENT`. Au premier démarrage, l’application crée le compte administrateur configuré par `ADMIN_USERNAME` et `ADMIN_PASSWORD` s’il n’existe pas et lui attribue le rôle `ADMIN`. Son mot de passe est encodé avant stockage. Cette première étape fournit le modèle de données et l’authentification persistante; l’administration des comptes et les espaces propres au secrétariat, aux enseignants, aux élèves et aux parents restent à réaliser.

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

## Déploiement provisoire sur Railway et Vercel

Railway peut héberger l'API Spring Boot et PostgreSQL; Vercel héberge le frontend et relaie `/api/*` vers l'API. Le proxy permet aux cookies de session de rester sur le domaine du frontend.

### API et base PostgreSQL sur Railway

1. Dans Railway, crée un projet puis ajoute un service **PostgreSQL**.
2. Ajoute au projet un service depuis le dépôt GitHub `daphneeewolo-code/projet_erfolg_zentrum`, branche `main`. Railway détecte le `Dockerfile` à la racine.
3. Configure l'API pour écouter le port injecté `PORT` (déjà pris en charge par l'application). Dans les réglages du service, définis le health check sur `/api/config` et génère un domaine public HTTPS.
4. Dans les variables du service API, configure `SPRING_PROFILES_ACTIVE=postgres`, `DATABASE_HOST=${{Postgres.PGHOST}}`, `DATABASE_PORT=${{Postgres.PGPORT}}`, `DATABASE_NAME=${{Postgres.PGDATABASE}}`, `DATABASE_USERNAME=${{Postgres.PGUSER}}` et `DATABASE_PASSWORD=${{Postgres.PGPASSWORD}}`. Remplace `Postgres` si tu as donné un autre nom au service de base de données.
5. Configure `ADMIN_PASSWORD` avec un secret unique conservé dans un gestionnaire de mots de passe; `ADMIN_USERNAME` vaut `admin` par défaut. Ne réutilise pas le mot de passe local de démonstration.

### Frontend sur Vercel

1. Importe le même dépôt dans Vercel, avec **Root Directory** `frontend`, le preset **Vite** et le répertoire de sortie `dist`.
2. Ajoute la variable Vercel `BACKEND_URL` avec le domaine HTTPS de l'API Railway, par exemple `https://nom-du-service.up.railway.app`, puis redéploie.
3. Vérifie `/api/config`, une inscription fictive, la connexion administrateur et l'accès refusé aux pages protégées sans connexion.

### Coûts, sauvegardes et limites avant production

Vérifie les tarifs dans Railway avant de créer les services : la documentation consultée indique un abonnement Hobby à 5 USD/mois avec 5 USD de crédits d'utilisation inclus, tandis que le forfait gratuit dispose de 1 USD de crédit mensuel; les ressources au-delà des crédits sont facturées selon leur consommation. Le montant réel dépend du temps d'exécution, de la mémoire, du CPU, du stockage et du trafic. Vercel et un nom de domaine personnalisé peuvent aussi entraîner des coûts.

Les sauvegardes Railway intégrées ont des rétentions distinctes : quotidiennes 6 jours, hebdomadaires 27 jours et mensuelles 89 jours. Elles ne satisfont pas à elles seules la décision du cahier des charges (sauvegarde quotidienne conservée 90 jours avec restauration testée chaque mois). Avant d'y mettre des données réelles d'élèves, il faut donc ajouter et tester une sauvegarde externe chiffrée avec la rétention convenue et documenter la restauration.

Ce déploiement reste un pilote : les modules élèves, responsables, enseignants, classes/années scolaires, présences géolocalisées, audit et autres fonctions du cahier ne sont pas encore tous implémentés. Le cahier demande aussi un domaine `.com`; le domaine Railway/Vercel fourni par défaut ne suffit pas à valider cette exigence. Ne pas saisir de données personnelles réelles avant d'avoir terminé ces points et la recette.