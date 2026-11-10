# LogiStock SA

Application de **gestion de stock et de ventes** pour une PME (projet fil rouge – Groupe N°1, Informatique de Gestion).
Elle permet de suivre les produits et leurs stocks, d'enregistrer les clients et les commandes, d'émettre des
factures PDF et de piloter l'activité avec un tableau de bord adapté à chaque métier.

## Sommaire
1. [Équipe](#équipe)
2. [Fonctionnalités](#fonctionnalités)
3. [Technologies et architecture](#technologies-et-architecture)
4. [Lancer l'application](#lancer-lapplication)
5. [Comptes de démonstration](#comptes-de-démonstration)
6. [Rôles et droits](#rôles-et-droits)
7. [Règles de gestion](#règles-de-gestion)
8. [API](#api)
9. [Tests](#tests)
10. [Organisation du dépôt et Git Flow](#organisation-du-dépôt-et-git-flow)

## Équipe

| Membre | Responsabilité |
| --- | --- |
| Mama Dit Kama Traoré | Scrum Master |
| Ahamadou Diarra | Lead back / architecte, qualité et sécurité |
| Hamzata Salahadine | Lead back / architecte |
| Gaoussou Cissé | Lead back / architecte |
| Founè Koita | Lead front / UI |
| Nouhoum Berthe | Lead front / UI |
| Mouhamoud Diarra | Lead front / UI |

## Fonctionnalités

- **Authentification et rôles** : connexion par e-mail et mot de passe (JWT), demande de compte par les
  utilisateurs, gestion des comptes par l'administrateur.
- **Stocks** : produits et catégories, recherche, mouvements d'entrée et de sortie tracés, alertes de seuil et
  de rupture (cloche dans l'application et notifications du navigateur).
- **Ventes et clients** : clients, commandes (en cours, validée, annulée), historique par client,
  facture PDF numérotée (`FAC-AAAA-NNNN`).
- **Tableau de bord** : indicateurs et graphiques selon le rôle de l'utilisateur.
- **Interface responsive** : utilisable sur téléphone, avec une page « À propos » (guide par rôle).

## Technologies et architecture

| Couche | Technologie |
| --- | --- |
| Frontend | Angular (composants autonomes, signaux), CSS sur mesure |
| Backend | Spring Boot 3.3 (Java 17), Spring Security, JWT, JPA |
| Base de données | MariaDB / MySQL |
| Conteneurs | Docker et Docker Compose (nginx pour le frontend) |
| Tests | JUnit 5 et Mockito |

Le backend suit une **architecture propre (Clean Architecture)** :

```
backend/src/main/java/com/logistock/
├── domain/            modèles, ports (interfaces) et exceptions métier
├── usecase/           règles de gestion (une classe par cas d'usage)
├── adapter/
│   ├── web/           contrôleurs REST et DTO
│   ├── persistence/   entités JPA et dépôts
│   └── pdf/           génération des factures PDF
└── infrastructure/    sécurité (JWT), configuration, données de démonstration
```

Les choix de technologies sont justifiés dans `docs/adr/001-choix-stack.md`.

## Lancer l'application

Deux méthodes sont décrites pas à pas dans **[LANCER-LOGISTOCK.md](LANCER-LOGISTOCK.md)**. En résumé :

### Avec Docker (recommandé, une seule installation : Docker Desktop)

```bash
git clone https://github.com/kamabayo2007-jpg/LogiStock-.git
cd LogiStock-
docker compose up --build
```

Puis ouvrir **http://localhost:4200**. L'API répond sur http://localhost:8080/api.
La base est créée automatiquement à partir de `docs/conception/schema.sql` et `seed.sql`.
Pour tout remettre à zéro : `docker compose down -v`.

### Sans Docker

Prérequis : Git, MariaDB/MySQL (par exemple XAMPP), JDK 17, Maven, Node.js 20+ et Angular CLI.

1. Créer la base `logistock`, puis importer `docs/conception/schema.sql` et `docs/conception/seed.sql`.
2. Backend : `cd backend && mvn spring-boot:run`
   (variables facultatives : `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ORIGINS`).
3. Frontend : `cd frontend && npm install && ng serve`, puis ouvrir http://localhost:4200.

## Comptes de démonstration

Créés au premier démarrage de l'API (les mots de passe sont stockés hachés avec BCrypt).

| Rôle | E-mail | Mot de passe |
| --- | --- | --- |
| Administrateur | admin@logistock.ml | Admin123! |
| Gestionnaire de stock | stock@logistock.ml | Stock123! |
| Commercial | vente@logistock.ml | Vente123! |

Un utilisateur peut aussi **demander un compte** depuis l'écran de connexion. Ce compte reste inactif
(rôle Commercial par défaut) jusqu'à son activation par un administrateur dans l'écran « Comptes ».
**Changer ces mots de passe et le `JWT_SECRET` avant toute mise en production.**

## Rôles et droits

Les droits sont **vérifiés côté serveur** (le masquage des menus du frontend n'est qu'un confort).

| Fonction | Administrateur | Gestionnaire de stock | Commercial |
| --- | :---: | :---: | :---: |
| Gérer les comptes | oui | non | non |
| Consulter produits et catégories | oui | oui | oui |
| Créer / modifier produits, enregistrer des mouvements | non | oui | non |
| Consulter les mouvements | oui | oui | non |
| Consulter clients et commandes | oui | non | oui |
| Créer clients, commandes, valider / annuler, factures | non | non | oui |

## Règles de gestion

- Le **stock ne peut jamais être négatif** : une sortie supérieure au stock est refusée.
- Une commande est créée « en cours » sans toucher au stock ; son **prix est figé** à la création.
- La **validation** est atomique (tout ou rien) : si une ligne manque de stock, rien n'est modifié.
  Elle génère les mouvements de sortie et la facture.
- Seule une commande « en cours » peut être validée ou annulée ; une commande validée ne s'annule pas.
- Une inscription crée un compte **inactif** : seul un administrateur peut l'activer.
- Les accès concurrents au stock sont protégés par un verrouillage pessimiste.

## API

Préfixe `/api`, authentification par en-tête `Authorization: Bearer <jeton>` (sauf les routes publiques).
Erreurs au format `{"message": "..."}` : 400 (données invalides), 401, 403, 404, 409 (règle de gestion).

| Domaine | Routes principales |
| --- | --- |
| Public | `POST /auth/login`, `POST /auth/inscription`, `GET /health` |
| Session | `GET /auth/me` |
| Comptes (admin) | `GET/POST /users`, `PUT /users/{id}`, `PATCH /users/{id}/actif` |
| Catalogue | `/produits` (+ `/alertes`, `/ruptures`, `?q=`), `/categories` |
| Stock | `GET/POST /mouvements` |
| Clients | `/clients`, `GET /clients/{id}/commandes` |
| Commandes | `/commandes`, `POST /commandes/{id}/validation`, `POST /commandes/{id}/annulation` |
| Factures | `GET /commandes/{id}/facture`, `GET /commandes/{id}/facture.pdf` |

Détails (corps des requêtes, exemples) : dossier [`docs/api/`](docs/api).

## Tests

```bash
cd backend
mvn test
```

Les tests automatisés couvrent les règles critiques : comptes et mots de passe hachés, inscription,
stock jamais négatif, mouvements, commandes (création, validation tout ou rien, annulation) et facture PDF.

## Organisation du dépôt et Git Flow

```
backend/     API Spring Boot (+ Dockerfile)
frontend/    application Angular (+ Dockerfile, nginx.conf)
docs/        cadrage, conception (MCD, schema.sql, seed.sql), ADR, documentation de l'API
scripts/     demarrer.sh / arreter.sh (lancement local sous Termux/Linux)
docker-compose.yml
```

- `main` : versions livrées ; `develop` : intégration ; `feature/*` : une branche par fonctionnalité.
- Aucun push direct sur `main` : tout passe par une Pull Request vers `develop`, relue avant fusion.
- Gestion de projet : tableau Trello « LogiStock SA – Projet fil rouge » (Scrum, jalons J1, J2, J3).
