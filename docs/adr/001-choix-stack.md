# ADR-001 : Choix de la pile technologique

- **Statut :** Accepté
- **Date :** 05/11/2026
- **Décideurs :** équipe LogiStock-SA (Groupe N°1)

## Contexte

LogiStock SA doit remplacer ses tableurs et cahiers papier par une application web de gestion (utilisateurs, stocks, ventes, clients, tableau de bord). Le cahier des charges impose :

- une séparation stricte entre frontend et backend, reliés par une API REST ;
- une base de données relationnelle (MySQL/MariaDB) ;
- une authentification par token JWT et un contrôle d'accès par rôles (RBAC) appliqué au niveau de l'API ;
- Git (GitFlow), Docker et Docker Compose ;
- la génération de factures PDF et quelques tests automatisés.

Le projet doit être livré en 12 séances, par une équipe de 7 étudiants, et **présenté sur un ordinateur équipé de XAMPP**.

## Décision

| Couche | Choix | Rôle |
|---|---|---|
| Frontend | Angular (TypeScript) | Interface web (pages, formulaires, graphiques) |
| Backend | Spring Boot 3 (Java 17) | API REST |
| Sécurité | Spring Security + JWT (jjwt) + BCrypt | Authentification, rôles, mots de passe hachés |
| Accès aux données | Spring Data JPA (Hibernate) | Lecture et écriture en base |
| Base de données | MariaDB / MySQL | Stockage relationnel (MariaDB de XAMPP pour la démo) |
| Factures PDF | OpenPDF | Génération des factures |
| Tests | JUnit 5 + Mockito | Règles métier critiques |
| Build | Maven | Compilation et JAR exécutable |
| Conteneurs | Docker + Docker Compose | Livrable : base, backend, frontend |
| Versions | Git + GitHub (GitFlow) | Travail d'équipe |

Le backend suit la Clean Architecture demandée dans la charte, avec ces paquets :
`domain` (entités et ports) → `usecase` (règles de gestion) → `adapter` (web et persistance) → `infrastructure` (sécurité, configuration).

## Alternatives considérées

- **Node.js / Express + React :** démarrage plus rapide, mais moins de structure imposée par le framework ; la discipline de la Clean Architecture repose entièrement sur l'équipe.
- **PHP / Symfony :** naturel avec XAMPP, mais l'équipe travaille avec Java et Angular.
- **Spring Boot + Thymeleaf (pas de frontend séparé) :** plus simple, mais contraire à l'exigence de séparation stricte frontend / backend.

## Conséquences

**Avantages**
- Typage fort (Java et TypeScript) : beaucoup d'erreurs détectées à la compilation.
- Spring Security gère nativement JWT et contrôle d'accès par rôle.
- Le backend est livré sous forme d'un seul fichier JAR : l'ordinateur de démo n'a besoin que de Java, pas de Maven ni de Node.js.
- L'application Angular compilée (`ng build`) est un ensemble de fichiers statiques que l'Apache de XAMPP peut servir.

**Inconvénients et points d'attention**
- Plus de code répétitif qu'avec Node.js ; démarrage de l'API plus lent.
- Java 17 minimum sur l'ordinateur de présentation.
- **Driver de base de données :** on utilise le driver MariaDB, car les versions récentes du driver MySQL sont incompatibles avec les anciennes versions de MariaDB livrées dans XAMPP.
- La règle « vente refusée si stock insuffisant » doit être faite dans une transaction (`@Transactional`) pour éviter un stock négatif quand deux ventes arrivent en même temps.
